package org.dreamabout.sw.frp.be.module.accounting.service;

/**
 * Connection credentials encrypted by {@link ConnectorCredentialCipher}.
 *
 * @param keyVersion version of the key that encrypted them
 * @param ciphertext Base64 of the nonce followed by the AES-GCM ciphertext and tag
 */
public record EncryptedCredentials(int keyVersion, String ciphertext) {
}
