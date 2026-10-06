package org.dreamabout.sw.frp.be.module.accounting.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.dreamabout.sw.frp.be.module.accounting.domain.ExternalMappingKind;
import org.dreamabout.sw.frp.be.module.common.model.IdAwareEntity;

/**
 * Mapping of an external account or category of a connection to an accounting account. Refreshed from the source,
 * which may happen without a user, so it carries no user audit fields.
 */
@Entity
@Getter
@Setter
@Table(name = "acc_external_mapping")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AccExternalMappingEntity implements IdAwareEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "acc_external_mapping_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "acc_external_mapping_id_seq")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "connection_id", nullable = false, updatable = false, comment = "Connection of the source")
    private Long connectionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, updatable = false, comment = "ACCOUNT or CATEGORY")
    private ExternalMappingKind kind;

    @Column(name = "external_id", nullable = false, updatable = false, comment = "Id in the source")
    private String externalId;

    @Column(name = "external_name", nullable = false, comment = "Name in the source")
    private String externalName;

    @Column(name = "currency_code", comment = "Currency of an external account; NULL for categories")
    private String currencyCode;

    @Column(name = "account_id", comment = "Accounting account it is mapped to; NULL when unmapped")
    private Long accountId;

    @Column(name = "ignored", nullable = false, comment = "Records of an ignored account or category are skipped")
    private Boolean ignored = false;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;
}
