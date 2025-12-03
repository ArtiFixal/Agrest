#!/bin/bash
CLIENT_CERTS="certs/client"

export VAULT_ADDR="https://localhost:8200"
export VAULT_CACERT="certs/root/root-ca.crt"

red(){
  echo -e "\033[0;31m$1\033[0m"
}

if [ ! -s vault/unsealTokens.txt ]; then
    red "[Error]: Vault unseal token not found"
    exit 1;
fi
export VAULT_CLIENT_CERT="$CLIENT_CERTS/client.crt"
export VAULT_CLIENT_KEY="$CLIENT_CERTS/client.key"
UNSEAL_KEY=$(grep 'Unseal Key' vault/unsealTokens.txt | awk '{print $NF}')
vault operator unseal "$UNSEAL_KEY"