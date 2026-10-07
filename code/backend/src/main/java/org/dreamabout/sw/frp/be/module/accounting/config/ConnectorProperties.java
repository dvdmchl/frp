package org.dreamabout.sw.frp.be.module.accounting.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "frp.connector")
@Getter
@Setter
public class ConnectorProperties {
    /**
     * Secret encrypting connection credentials: any text (hashed with SHA-256) or a Base64 encoded 16, 24 or 32 byte
     * AES key used as is. Never commit it.
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

    /**
     * How many days back from today a synchronization fetches records. Records in this window that the source no
     * longer returns are marked deleted.
     */
    private int syncWindowDays = 90;

    /**
     * Delay of the next scheduled synchronization when the source is not ready to serve data yet.
     */
    private Duration syncNotReadyRetry = Duration.ofMinutes(30);

    /**
     * How many of the latest synchronization runs are kept per connection; older ones are deleted.
     */
    private int syncRunsKept = 50;
}
