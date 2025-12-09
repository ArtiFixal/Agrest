#!/bin/bash
# default variable values
export VAULT_ADDR="https://localhost:8200"
PROJECT_ROOT="$(pwd)"

CLIENT_CERTS="$PROJECT_ROOT/certs/client"
VAULT_CERTS="$PROJECT_ROOT/certs/vault"
SPRING_CERTS="$PROJECT_ROOT/certs/spring"
FRONTEND_CERTS="$PROJECT_ROOT/certs/frontend"
ROOT_CA_CERT="$PROJECT_ROOT/certs/root/root-ca.crt"
ROOT_CA_KEY="$PROJECT_ROOT/certs/root/root-ca.key"
SHM_CERTS="/dev/shm/vault/certs"
SHM_SECRETS="/dev/shm/vault/secrets"

red(){
  echo -e "\033[0;31m$1\033[0m"
}

yellow(){
  echo -ne "\033[1;33m$1\033[0m"
}

# Root CA
generateRootCA(){
  echo "[Info]: Creating Root CA"
  openssl ecparam -name secp384r1 -genkey -out "$ROOT_CA_KEY"
  
  openssl req -new -x509 -days 7300 \
    -key "$ROOT_CA_KEY" \
    -out "$ROOT_CA_CERT" \
    -sha384 \
    -subj "/C=PL/O=Agrest/CN=Agrest Root CA"

  export VAULT_CACERT="$ROOT_CA_CERT"
  echo "[Info]: Root CA created"
}

generateClientCert(){
  echo "[Info]: Creating Client cert"
  openssl ecparam -name secp384r1 -genkey -out "$CLIENT_CERTS/client.key"

  # Create client cert config if no any
  if [ ! -s "$CLIENT_CERTS/client.cfg" ]; then
    cat > "$CLIENT_CERTS/client.cfg" << EOF
[req]
distinguished_name = req_distinguished_name
req_extensions = v3_req
prompt = no

[req_distinguished_name]
C = PL
O = Agrest
CN = Vault client

[v3_req]
keyUsage = critical, digitalSignature, keyEncipherment
extendedKeyUsage = clientAuth
EOF
  echo "[Info]: Created client cert config"
  fi

  # CSR for Client TLS
  openssl req -new \
    -key "$CLIENT_CERTS/client.key" \
    -out "$CLIENT_CERTS/client.csr" \
    -config "$CLIENT_CERTS/client.cfg"

  openssl x509 -req \
    -in "$CLIENT_CERTS/client.csr" \
    -CA "$ROOT_CA_CERT" \
    -CAkey "$ROOT_CA_KEY" \
    -CAcreateserial \
    -out "$CLIENT_CERTS/client.crt" \
    -days 365 \
    -sha384 \
    -copy_extensions copy

  rm "$CLIENT_CERTS/client.csr"
  echo "[Info]: Client certs created"
  yellow "Enter password which will be used for client pkcs12 export: "
  read -s clientPass
  echo
  openssl pkcs12 -export -inkey "$CLIENT_CERTS/client.key" -in "$CLIENT_CERTS/client.crt" \
    -out "$CLIENT_CERTS/client.p12" \
    -passout "pass:$clientPass"
}

generateVaultCert(){
  echo "[Info]: Creating Vault certs"  
  openssl ecparam -name secp384r1 -genkey -out $VAULT_CERTS/vault-server.key

  # Create server cert config if no any
  if [ ! -s certs/vault/vault-server-csr.cfg ]; then
    cat > certs/vault/vault-server-csr.cfg <<EOF
[req]
distinguished_name = req_distinguished_name
req_extensions = v3_req
prompt = no

[req_distinguished_name]
C = PL
O = Agrest
CN = Agrest Vault

[v3_req]
keyUsage = critical, digitalSignature, keyEncipherment
extendedKeyUsage = serverAuth, clientAuth
subjectAltName = @alt_names

[alt_names]
DNS.1 = vault
DNS.2 = localhost
IP.1 = 127.0.0.1
EOF
  echo "[Info]: Created Vault server cert config"
  fi

  # CSR for Vault TLS
  openssl req -new \
    -key certs/vault/vault-server.key \
    -out certs/vault/vault-server.csr \
    -config certs/vault/vault-server-csr.cfg

  openssl x509 -req \
    -in certs/vault/vault-server.csr \
    -CA "$ROOT_CA_CERT" \
    -CAkey "$ROOT_CA_KEY" \
    -CAcreateserial \
    -out certs/vault/vault-server.crt \
    -days 365 \
    -sha384 \
    -copy_extensions copy
    rm certs/vault/vault-server.csr

    # For dev only!
    chmod 644 certs/vault/vault-server.key
    echo "[Info]: Vault certs created"
}

createVault(){
  echo "[Info]: Creating Vault container"

  # PKI doesn't exist yet so use Root CA
  cp "$ROOT_CA_CERT" certs/vault/ca_chain.pem
  export VAULT_CLIENT_CERT="$CLIENT_CERTS/client.crt"
  export VAULT_CLIENT_KEY="$CLIENT_CERTS/client.key"
  export VAULT_SKIP_VERIFY="false"

  if [ ! -s vault/vault-config.hcl ]; then
  cat > vault/vault-config.hcl <<EOF
disable_mlock = true
ui = true

storage "file" {
  path = "/vault/data"
}

listener "tcp" {
  address = "0.0.0.0:8200"
  tls_cert_file = "/vault/certs/vault-server.crt"
  tls_key_file = "/vault/certs/vault-server.key"
  tls_client_ca_file = "/vault/ca.pem"
  tls_require_and_verify_client_cert = true
  tls_min_version = "tls12"
}

api_addr = "https://vault:8200"
cluster_addr = "https://vault:8201"
EOF
  fi

  docker-compose up -d vault --wait
  if [ $? -ne 0 ]; then
    red "[Error]: Failed to create healthy container"
    exit $?
  fi
  echo "[Info]: Vault container up"

  if [ ! -s vault/unsealTokens.txt ]; then
    vault operator init -key-shares=1 -key-threshold=1 > vault/unsealTokens.txt
  fi
  echo "[Info]: Vault container unseal init"
  UNSEAL_KEY=$(grep 'Unseal Key' vault/unsealTokens.txt | awk '{print $NF}')
  vault operator unseal "$UNSEAL_KEY"
  export VAULT_TOKEN=$(grep 'Initial Root Token:' vault/unsealTokens.txt | awk '{print $NF}')
  echo "[Info]: Vault container unsealed"
  vault secrets enable -version=2 kv
}

issueVaultClientCert(){
  vault write -format=json pki-intermediate/issue/agrest-app-vault \
    common_name="agrest-app" \
    token_ttl=1h \
    token_max_ttl=2h \
    ext_key_usage=ClientAuth > "$RESPONSE"
  return $?;
}

setupPKI(){
  echo "[Info]: Setting up Vault CA"

  vault auth enable cert
  vault secrets enable -path=pki-intermediate pki

  vault write pki-intermediate/config/urls \
    issuing_certificates="https://vault:8200/v1/pki-intermediate/ca" \
    crl_distribution_points="https://vault:8200/v1/pki-intermediate/crl" \
    ocsp_servers="https://vault:8200/v1/pki-intermediate/ocsp"

  vault secrets tune -max-lease-ttl=8760h pki-intermediate

  # generate Vault CSR
  vault write -field=csr pki-intermediate/intermediate/generate/internal \
    common_name="Vault Intermediate CA" \
    issuer_name="Agrest Vault CA" \
    key_type=ec \
    key_bits=384 \
    organization="Agrest Vault" > certs/vault/vault_int.csr

  if  [ ! -s certs/vault/v3_intermediate_ca.ext ]; then
    cat > certs/vault/v3_intermediate_ca.ext <<EOF
[v3_intermediate_ca]
subjectKeyIdentifier = hash
authorityKeyIdentifier = keyid:always,issuer
basicConstraints = critical, CA:true, pathlen:1
keyUsage = critical, digitalSignature, cRLSign, keyCertSign
EOF
  fi

  # Sign Vault CA
  openssl x509 -req -in certs/vault/vault_int.csr \
    -CA "$ROOT_CA_CERT" \
    -CAkey "$ROOT_CA_KEY" \
    -CAcreateserial \
    -out certs/vault/vault-int-cert.pem \
    -days 3650 \
    -sha384 \
    -extfile certs/vault/v3_intermediate_ca.ext \
    -extensions v3_intermediate_ca

  rm certs/vault/vault_int.csr

  # Concatenate chain
  cat certs/vault/vault-int-cert.pem "$ROOT_CA_CERT" > certs/vault/ca_chain.pem
  # Import signed cert
  vault write pki-intermediate/intermediate/set-signed certificate=@certs/vault/ca_chain.pem
}

createSpringCerts(){
  echo "[Info]: Creating vault cert for spring"
  vault write pki-intermediate/roles/agrest-app-vault \
    allowed_domains="agrest-app,localhost" \
    allow_subdomains=true \
    allow_localhost=true \
    allow_bare_domains=true \
    allow_ip_sans=true \
    server_flag=true \
    client_flag=true \
    key_type=ec \
    key_bits=384 \
    max_ttl="7d" \
    require_cn=false

  # Create spring policy
  vault policy write agrest-app-policy - <<EOF
  path "kv/data/agrest-app/*" {
      capabilities = ["create", "read", "update"]
  }

  path "kv/metadata/agrest-app/*" {
    capabilities = ["read","list"]
  }
EOF

  vault write auth/cert/certs/agrest-app-vault \
    display_name="agrest-app" \
    certificate=@certs/vault/vault-int-cert.pem \
    policies="agrest-app-policy" \
    ttl=1h

  echo "[Info]: Creating server cert for spring"  
  # Create role for server cert
  vault write pki-intermediate/roles/agrest-app-server \
    allowed_domains="localhost,agrest-server" \
    allow_localhost=true \
    allow_subdomains=true \
    allow_bare_domains=true \
    key_type=ec \
    key_bits=384 \
    ext_key_usage=ServerAuth \
    max_ttl=365d 
}

issueSpringCerts(){
  RESPONSE="$SHM_CERTS/cert-resp.json"

  issueVaultClientCert
  if [ $? -ne 0 ]; then
    red "[Error]: Failed to issue Vault client cert. Retrying after 31s..."
    # wait for
    sleep 31
    issueVaultClientCert
  fi

  cat "$RESPONSE" | grep -oP '"certificate"\s*:\s*"\K[^"]+' | sed 's/\\n/\n/g' \
    > "$SHM_CERTS/agrest-client.crt"
  cat "$RESPONSE" | grep -oP '"private_key"\s*:\s*"\K[^"]+' | sed 's/\\n/\n/g' \
    > "$SHM_CERTS/agrest-client.key"
  cat "$RESPONSE" | grep -oP '"issuing_ca"\s*:\s*"\K[^"]+' | sed 's/\\n/\n/g' \
    > "$SHM_CERTS/agrest-client.pem"

  vault write -format=json pki-intermediate/issue/agrest-app-server \
    common_name="localhost" \
    alt_names="localhost,agrest-server" > "$RESPONSE"

  cat "$RESPONSE" | grep -oP '"certificate"\s*:\s*"\K[^"]+' | sed 's/\\n/\n/g' \
    > "$SHM_CERTS/agrest-server.crt"
  cat "$RESPONSE" | grep -oP '"private_key"\s*:\s*"\K[^"]+' | sed 's/\\n/\n/g' \
    > "$SHM_CERTS/agrest-server.key"
  cat "$RESPONSE" | grep -oP '"issuing_ca"\s*:\s*"\K[^"]+' | sed 's/\\n/\n/g' \
    > "$SHM_CERTS/agrest-server-ica.pem"
  
  rm "$RESPONSE"
  cat "$SHM_CERTS/agrest-server.crt" "$SHM_CERTS/agrest-server-ica.pem" > "$SHM_CERTS/agrest-server.pem"
  echo "[Info]: Issued spring certs"
}

createFrontendCertRole(){
  echo "[Info]: Creating frontend cert"
  vault write pki-intermediate/roles/agrest-app-frontend \
    allowed_domains="localhost,agrest-app" \
    allow_localhost=true \
    allow_subdomains=true \
    allow_bare_domains=true \
    key_type=ec \
    key_bits=384 \
    ext_key_usage=ServerAuth \
    max_ttl=365d
}

issueFrontendCert(){
  RESPONSE="$FRONTEND_CERTS/cert-resp.json"

  vault write -format=json pki-intermediate/issue/agrest-app-frontend \
    common_name="localhost" \
    alt_names="localhost,agrest-app" > "$RESPONSE"

  cat "$RESPONSE" | grep -oP '"certificate"\s*:\s*"\K[^"]+' | sed 's/\\n/\n/g' \
    > "$FRONTEND_CERTS/agrest-frontend.crt"
  cat "$RESPONSE" | grep -oP '"private_key"\s*:\s*"\K[^"]+' | sed 's/\\n/\n/g' \
    > "$FRONTEND_CERTS/agrest-frontend.key"
  cat "$RESPONSE" | grep -oP '"issuing_ca"\s*:\s*"\K[^"]+' | sed 's/\\n/\n/g' \
    > "$FRONTEND_CERTS/agrest-frontend-ica.pem"

  cat "$FRONTEND_CERTS/agrest-frontend.crt" "$FRONTEND_CERTS/agrest-frontend-ica.pem" > "$FRONTEND_CERTS/agrest-frontend.pem"
  rm "$RESPONSE"
  echo "[Info]: Issued frontend cert"
}

createStores(){
  umask 177
  PASS_SERVER=$(openssl rand -base64 16)

  # Tmp p12 for ssl
  echo "[Info]: Creating agrest-keystore"
  openssl pkcs12 -export -inkey "$SHM_CERTS/agrest-server.key" \
    -in "$SHM_CERTS/agrest-server.pem" \
    -out "$SHM_CERTS/agrest-server.p12" \
    -name "ssl-cert" \
    -passout "pass:$PASS_SERVER"
  rm "$SHM_CERTS/agrest-server.crt" "$SHM_CERTS/agrest-server.key"
  echo "[Info]: Created agrest-server P12"

  PASS_CLIENT=$(openssl rand -base64 16)
  # Tmp p12 for vault
  openssl pkcs12 -export -inkey "$SHM_CERTS/agrest-client.key" \
    -in "$SHM_CERTS/agrest-client.crt" \
    -out "$SHM_CERTS/agrest-client.p12" \
    -name "vault-cert" \
    -passout "pass:$PASS_CLIENT"
  rm "$SHM_CERTS/agrest-client.crt" "$SHM_CERTS/agrest-client.key"
  echo "[Info]: Created agrest-vault P12"

  yellow "Enter password for new KeyStore: "
  read -s PASS_KEY_STORE
  echo
  # Merge p12 to keystore
  keytool -importkeystore -srckeystore "$SHM_CERTS/agrest-server.p12" \
    -srcstoretype PKCS12 \
    -destkeystore agrest-backend/agrest-keystore.p12 \
    -deststoretype PKCS12 \
    -srcstorepass "$PASS_SERVER" \
    -deststorepass "$PASS_KEY_STORE"

  keytool -importkeystore -srckeystore "$SHM_CERTS/agrest-client.p12" \
    -srcstoretype PKCS12 \
    -destkeystore agrest-backend/agrest-keystore.p12 \
    -deststoretype PKCS12 \
    -srcstorepass "$PASS_CLIENT" \
    -deststorepass "$PASS_KEY_STORE"

  rm "$SHM_CERTS/agrest-server.p12" "$SHM_CERTS/agrest-client.p12"
  echo "[Info]: agrest-keystore created"

  yellow "Enter password for new TrustStore: "
  read -s PASS_TRUST_STORE
  echo

  echo "[Info]: Creating agrest-truststore"
  keytool -import -trustcacerts \
    -alias root-ca \
    -file "$VAULT_CACERT" \
    -keystore agrest-backend/agrest-truststore.p12 \
    -storetype PKCS12 \
    -storepass "$PASS_TRUST_STORE"
  echo "[Info]: agrest-truststore created"

  umask 177
  echo "[Info]: Encrypting .env"
  echo "KeyStore: $PASS_KEY_STORE" > "$SHM_SECRETS/.env"
  echo "$PASS_KEY_STORE" > "$SHM_SECRETS/keystore"
  echo "TrustStore: $PASS_TRUST_STORE" >> "$SHM_SECRETS/.env"
  echo "$PASS_TRUST_STORE" > "$SHM_SECRETS/truststore"

  gpg --batch --encrypt --recipient "$HOSTNAME@localhost" --output agrest-backend/.enc.env "$SHM_SECRETS/.env"
  rm "$SHM_SECRETS/.env"
  umask 0022
}

createEncKeypair(){
  umask 0022
  echo "[Info]: Creating GPG keypair"
  gpg --batch --quick-gen-key "Agrest-app config key <$HOSTNAME@localhost>" ed25519 cert,sign 0
  KEY_FP=$(gpg --list-keys --with-colons "$HOSTNAME@localhost" | awk -F: '/^fpr:/ {print $10; exit}')
  gpg --quick-add-key "$KEY_FP" cv25519 encr 0
  # Export public key
  gpg -a --export "$HOSTNAME@localhost" > agrest-backend/storeKey.asc
}

setupUserPass(){
  vault auth enable userpass
  vault policy write admin-policy - <<EOF
path "*" {
  capabilities = ["create", "read", "update", "delete", "list", "sudo"]
}
EOF

  # Create admin
  yellow "Enter password for Vault admin account: "
  read -s password
  echo
  vault write auth/userpass/users/admin \
    password="$password" \
    policies="admin-policy" \
    token_ttl="30m" \
    token_max_ttl="1h"
  echo "[Info]: Admin created"
}