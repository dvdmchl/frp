package org.dreamabout.sw.frp.be.module.accounting.service;

import org.dreamabout.sw.frp.be.module.accounting.config.ConnectorProperties;
import org.dreamabout.sw.frp.be.module.accounting.connector.ConnectorCredentials;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Encrypts connection credentials at rest with AES-GCM. The key comes from {@code frp.connector.encryption-key}:
 * a Base64 encoded 16, 24 or 32 byte AES key is used as is, any other text is hashed with SHA-256 into a 256-bit key.
 * The key's version is stored with each ciphertext, so credentials encrypted with a retired key
 * ({@code frp.connector.previous-encryption-keys}) stay readable after a rotation.
 * Error messages never contain credential values.
 */
@Component
public final class ConnectorCredentialCipher {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final Set<Integer> AES_KEY_BYTES = Set.of(16, 24, 32);
    private static final TypeReference<Map<String, String>> VALUES_TYPE = new TypeReference<>() {
    };

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final SecureRandom random = new SecureRandom();
    private final int currentKeyVersion;
    private final Map<Integer, SecretKey> keysByVersion;

    public ConnectorCredentialCipher(ConnectorProperties properties) {
        this.currentKeyVersion = properties.getEncryptionKeyVersion();
        Map<Integer, SecretKey> keys = new HashMap<>();
        properties.getPreviousEncryptionKeys().forEach((version, key) -> keys.put(version, toKey(version, key)));
        String currentKey = properties.getEncryptionKey();
        if (currentKey != null && !currentKey.isBlank()) {
            keys.put(currentKeyVersion, toKey(currentKeyVersion, currentKey));
        }
        this.keysByVersion = Map.copyOf(keys);
    }

    public EncryptedCredentials encrypt(ConnectorCredentials credentials) {
        SecretKey key = keysByVersion.get(currentKeyVersion);
        if (key == null) {
            throw new IllegalStateException(
                    "Connector credentials cannot be encrypted: frp.connector.encryption-key is not configured");
        }
        byte[] nonce = new byte[NONCE_BYTES];
        random.nextBytes(nonce);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            byte[] ciphertext = cipher.doFinal(JSON.writeValueAsBytes(credentials.values()));
            byte[] data = ByteBuffer.allocate(NONCE_BYTES + ciphertext.length).put(nonce).put(ciphertext).array();
            return new EncryptedCredentials(currentKeyVersion, Base64.getEncoder().encodeToString(data));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt connector credentials", e);
        }
    }

    public ConnectorCredentials decrypt(EncryptedCredentials encrypted) {
        SecretKey key = keysByVersion.get(encrypted.keyVersion());
        if (key == null) {
            throw new IllegalStateException("No encryption key configured for key version " + encrypted.keyVersion());
        }
        try {
            byte[] data = Base64.getDecoder().decode(encrypted.ciphertext());
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, data, 0, NONCE_BYTES));
            byte[] plaintext = cipher.doFinal(data, NONCE_BYTES, data.length - NONCE_BYTES);
            return new ConnectorCredentials(JSON.readValue(plaintext, VALUES_TYPE));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Failed to decrypt connector credentials with key version " + encrypted.keyVersion(), e);
        }
    }

    private static SecretKey toKey(int version, String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("Connector encryption key version " + version + " must not be blank");
        }
        return new SecretKeySpec(decodeAesKey(key).orElseGet(() -> sha256(key)), "AES");
    }

    private static Optional<byte[]> decodeAesKey(String key) {
        try {
            byte[] bytes = Base64.getDecoder().decode(key);
            return AES_KEY_BYTES.contains(bytes.length) ? Optional.of(bytes) : Optional.empty();
        } catch (IllegalArgumentException _) {
            return Optional.empty();
        }
    }

    private static byte[] sha256(String key) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
