package org.dreamabout.sw.frp.be.module.accounting.service;

import org.dreamabout.sw.frp.be.module.accounting.config.ConnectorProperties;
import org.dreamabout.sw.frp.be.module.accounting.connector.ConnectorCredentials;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConnectorCredentialCipherTest {

    private static final String KEY_V1 = key((byte) 1);
    private static final String KEY_V2 = key((byte) 2);
    private static final ConnectorCredentials CREDENTIALS = new ConnectorCredentials(Map.of("token", "secret-token"));

    @Test
    void shouldDecryptWhatItEncrypted() {
        var cipher = cipher(KEY_V1, 1, Map.of());

        var encrypted = cipher.encrypt(CREDENTIALS);

        assertThat(cipher.decrypt(encrypted)).isEqualTo(CREDENTIALS);
    }

    @Test
    void shouldStoreCurrentKeyVersionWithCiphertext() {
        var cipher = cipher(KEY_V2, 2, Map.of(1, KEY_V1));

        var encrypted = cipher.encrypt(CREDENTIALS);

        assertThat(encrypted.keyVersion()).isEqualTo(2);
    }

    @Test
    void shouldNotRevealPlaintextAndUseFreshNonceForEachEncryption() {
        var cipher = cipher(KEY_V1, 1, Map.of());

        var first = cipher.encrypt(CREDENTIALS);
        var second = cipher.encrypt(CREDENTIALS);

        assertThat(first.ciphertext()).doesNotContain("secret-token").isNotEqualTo(second.ciphertext());
    }

    @Test
    void shouldDecryptWithPreviousKeyAfterRotation() {
        var encrypted = cipher(KEY_V1, 1, Map.of()).encrypt(CREDENTIALS);
        var rotated = cipher(KEY_V2, 2, Map.of(1, KEY_V1));

        assertThat(rotated.decrypt(encrypted)).isEqualTo(CREDENTIALS);
    }

    @Test
    void shouldFailWhenKeyVersionIsNotConfigured() {
        var encrypted = cipher(KEY_V1, 1, Map.of()).encrypt(CREDENTIALS);
        var rotated = cipher(KEY_V2, 2, Map.of());

        assertThatThrownBy(() -> rotated.decrypt(encrypted))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("key version 1");
    }

    @Test
    void shouldFailWhenCiphertextWasTamperedWith() {
        var cipher = cipher(KEY_V1, 1, Map.of());
        var encrypted = cipher.encrypt(CREDENTIALS);
        var bytes = Base64.getDecoder().decode(encrypted.ciphertext());
        bytes[bytes.length - 1] ^= 1;
        var tampered = new EncryptedCredentials(1, Base64.getEncoder().encodeToString(bytes));

        assertThatThrownBy(() -> cipher.decrypt(tampered))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining("secret-token");
    }

    @Test
    void shouldFailToEncryptWhenKeyIsNotConfigured() {
        var cipher = cipher("", 1, Map.of());

        assertThatThrownBy(() -> cipher.encrypt(CREDENTIALS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("frp.connector.encryption-key");
    }

    @Test
    void shouldRejectKeyOfInvalidLength() {
        var shortKey = Base64.getEncoder().encodeToString(new byte[10]);

        Map<Integer, String> noPreviousKeys = Map.of();

        assertThatThrownBy(() -> cipher(shortKey, 1, noPreviousKeys))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("version 1");
    }

    private static ConnectorCredentialCipher cipher(String key, int version, Map<Integer, String> previousKeys) {
        var properties = new ConnectorProperties();
        properties.setEncryptionKey(key);
        properties.setEncryptionKeyVersion(version);
        properties.setPreviousEncryptionKeys(previousKeys);
        return new ConnectorCredentialCipher(properties);
    }

    private static String key(byte fill) {
        var bytes = new byte[32];
        Arrays.fill(bytes, fill);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
