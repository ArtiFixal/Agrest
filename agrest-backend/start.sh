#!/bin/bash
CLIENT_CERTS="certs/client"
VAULT_CERTS="certs/vault"

export VAULT_ADDR="https://localhost:8200"
export VAULT_CACERT="certs/root/root-ca.crt"
export VAULT_CLIENT_CERT="$CLIENT_CERTS/client.crt"
export VAULT_CLIENT_KEY="$CLIENT_CERTS/client.key"
export VAULT_SKIP_VERIFY="false"

cd ..
vault status > /dev/null
if [ $? -ne 0 ]; then
  echo "[Info]: Unsealing vault"
  bash ./unsealVaultDev.sh
fi
cd agrest-backend

bash ./extractKeys.sh

# Configure for your needs
java -Dserver.ssl.key-store=agrest-keystore.p12 \
  -Dserver.ssl.trust-store=agrest-truststore.p12 \
  -Dsecret.keystore.path=/dev/shm/vault/secrets/keystore \
  -Dsecret.truststore.path=/dev/shm/vault/secrets/truststore \
  -Dapp.security.cors.allowed-origins[0]=https://localhost:5173 \
  -jar target/Agrest-0.1.jar
