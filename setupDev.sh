#!/bin/bash
###
### Script setting up Vault for developement purposes.
###
export VAULT_ADDR="https://localhost:8200"
PROJECT_ROOT="$(pwd)"

CLIENT_CERTS="$PROJECT_ROOT/certs/client"
VAULT_CERTS="$PROJECT_ROOT/certs/vault"
SPRING_CERTS="$PROJECT_ROOT/certs/spring"
ROOT_CA_CERT="$PROJECT_ROOT/certs/root/root-ca.crt"
ROOT_CA_KEY="$PROJECT_ROOT/certs/root/root-ca.key"
SHM_CERTS="/dev/shm/vault/certs"
SHM_SECRETS="/dev/shm/vault/secrets"

# Import functions
source scriptFunctions.sh

mkdir vault-data
chmod 777 vault-data
mkdir vault
mkdir -p certs/{root,client,vault,frontend}
mkdir -p /dev/shm/vault/{secrets,certs}
chmod 700 /dev/shm/vault /dev/shm/vault/certs /dev/shm/vault/secrets


generateRootCA
generateClientCert
generateVaultCert
createVault
setupPKI
createSpringCerts
issueSpringCerts
createEncKeypair
createStores
createFrontendCertRole
issueFrontendCert
setupUserPass

# Revoke root token
vault token revoke -self