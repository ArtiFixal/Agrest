#!/bin/bash
SHM_SECRETS="/dev/shm/vault/secrets"

umask 077
mkdir -p /dev/shm/vault/{secrets,certs}
umask 177
if [ -n "${KEY_PASSWORD}" ]; then
  unenc=$(gpg --passphrase "${KEY_PASSWORD}" -d .enc.env)
else
  unenc=$(gpg -d .enc.env)
fi

echo -e "$unenc" | grep KeyStore: | awk '{print $2}' > "$SHM_SECRETS/keystore"
echo -e "$unenc" | grep TrustStore: | awk '{print $2}' > "$SHM_SECRETS/truststore"

umask 0022