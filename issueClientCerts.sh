#!/bin/bash
###
### Re/Create certificates
###
CLIENT_CERTS="certs/client"
SHM_CERTS="/dev/shm/vault/certs"
SHM_SECRETS="/dev/shm/vault/secrets"

export VAULT_ADDR="https://localhost:8200"
export VAULT_CACERT="certs/root/root-ca.crt"
export VAULT_CLIENT_CERT="$CLIENT_CERTS/client.crt"
export VAULT_CLIENT_KEY="$CLIENT_CERTS/client.key"
export VAULT_SKIP_VERIFY="false"


umask 077
mkdir -p /dev/shm/vault/{secrets,certs}
umask 177

red(){
  echo -e "\033[0;31m$1\033[0m"
}

yellow(){
  echo -ne "\033[1;33m$1\033[0m"
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

  echo "[Info]: Encrypting .env"
  echo "KeyStore: $PASS_KEY_STORE" > "$SHM_SECRETS/.env"
  echo "$PASS_KEY_STORE" > "$SHM_SECRETS/keystore"
  echo "TrustStore: $PASS_TRUST_STORE" >> "$SHM_SECRETS/.env"
  echo "$PASS_TRUST_STORE" > "$SHM_SECRETS/truststore"

  gpg --batch --encrypt --recipient "$HOSTNAME@localhost" --output agrest-backend/.enc.env "$SHM_SECRETS/.env"
  rm "$SHM_SECRETS/.env"
  umask 0022
}

vault status > /dev/null
if [ $? -ne 0 ]; then
  echo "[Info]: Unsealing vault"
  bash ./unsealVaultDev.sh
fi

RESPONSE="$SHM_CERTS/cert-resp.json"

yellow "Enter user password: "
read -s password
vault login -method=userpass username=admin password="$password"

  vault write -format=json pki-intermediate/issue/agrest-app-vault \
    common_name="agrest-app" \
    token_ttl=1h \
    token_max_ttl=2h \
    ext_key_usage=ClientAuth > "$RESPONSE"

  cat "$RESPONSE" | grep -oP '"certificate"\s*:\s*"\K[^"]+' | sed 's/\\n/\n/g' \
    > "$SHM_CERTS/agrest-client.crt"
  cat "$RESPONSE" | grep -oP '"private_key"\s*:\s*"\K[^"]+' | sed 's/\\n/\n/g' \
    > "$SHM_CERTS/agrest-client.key"
  cat "$RESPONSE" | grep -oP '"issuing_ca"\s*:\s*"\K[^"]+' | sed 's/\\n/\n/g' \
    > "$SHM_CERTS/agrest-client.pem"

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
createStores