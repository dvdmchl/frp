package org.dreamabout.sw.frp.be.module.accounting.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "frp.connector")
@Getter
@Setter
public class ConnectorProperties {
    /**
     * Base64 encoded AES key (16, 24 or 32 bytes) encrypting connection credentials. Never commit it.
     */
    private String encryptionKey;

    /**
     * Version of {@link #encryptionKey}; stored with every ciphertext. Increase it when rotating the key.
     */
    private int encryptionKeyVersion = 1;

    /**
     * Retired keys by version, kept so credentials encrypted before a rotation can still be decrypted.
     */
    private Map<Integer, String> previousEncryptionKeys = new HashMap<>();
}
