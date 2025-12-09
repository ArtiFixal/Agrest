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


# Import functions
source scriptFunctions.sh

umask 077
mkdir -p /dev/shm/vault/{secrets,certs}
umask 177

vault status > /dev/null
if [ $? -ne 0 ]; then
  echo "[Info]: Unsealing vault"
  bash ./unsealVaultDev.sh
fi

yellow "Enter admin password: "
read -s password
vault login -method=userpass username=admin password="$password" > /dev/null

RESPONSE="$SHM_CERTS/cert-resp.json"

issueSpringCerts
createStores

vault token revoke -self