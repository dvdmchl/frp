package org.dreamabout.sw.frp.be.module.accounting.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.dreamabout.sw.frp.be.module.common.model.AuditableEntity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;

@Entity
@Getter
@Setter
@Table(name = "acc_connection")
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class AccConnectionEntity extends AuditableEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "acc_connection_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "acc_connection_id_seq")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "connector_type", nullable = false, updatable = false,
            comment = "Type of the connector serving this connection, e.g. 'WALLET'")
    private String connectorType;

    @Column(name = "name", nullable = false, unique = true, comment = "User-defined name of the connection")
    private String name;

    @Column(name = "enabled", nullable = false, comment = "Whether the connection is synchronized")
    private Boolean enabled = true;

    @Column(name = "credentials",
            comment = "AES-GCM encrypted credentials (Base64 of nonce and ciphertext); NULL when not set")
    private String credentials;

    @Column(name = "credentials_key_version", comment = "Version of the key that encrypted the credentials")
    private Integer credentialsKeyVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sync_settings", nullable = false, comment = "Synchronization settings of the connection")
    private HashMap<String, String> syncSettings = new HashMap<>();

    @Column(name = "last_successful_sync_at", comment = "End of the last successful synchronization")
    private Instant lastSuccessfulSyncAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sync_state", comment = "Connector cursor and state of the synchronization")
    private HashMap<String, String> syncState;
}
