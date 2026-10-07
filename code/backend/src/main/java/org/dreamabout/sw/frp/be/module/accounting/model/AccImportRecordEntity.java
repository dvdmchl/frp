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
import org.dreamabout.sw.frp.be.module.accounting.connector.ExternalRecordState;
import org.dreamabout.sw.frp.be.module.accounting.domain.ImportRecordStatus;
import org.dreamabout.sw.frp.be.module.common.model.IdAwareEntity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * External record in the import staging, unique per connection and external id. Written by synchronization, which
 * may run without a user, so it carries first/last seen times instead of the user audit fields.
 */
@Entity
@Getter
@Setter
@Table(name = "acc_import_record")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AccImportRecordEntity implements IdAwareEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "acc_import_record_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "acc_import_record_id_seq")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "connection_id", nullable = false, updatable = false, comment = "Connection the record came from")
    private Long connectionId;

    @Column(name = "external_id", nullable = false, updatable = false, comment = "Id of the record in the source")
    private String externalId;

    @Column(name = "external_account_id", nullable = false, comment = "Id of the account in the source")
    private String externalAccountId;

    @Column(name = "external_category_id", comment = "Id of the category in the source; NULL when uncategorized")
    private String externalCategoryId;

    @Column(name = "record_date", nullable = false, comment = "Date of the record")
    private LocalDate recordDate;

    @Column(name = "amount", nullable = false, comment = "Signed amount; negative = money going out of the account")
    private BigDecimal amount;

    @Column(name = "currency_code", nullable = false, comment = "Currency of the amount")
    private String currencyCode;

    @Column(name = "base_amount", comment = "Amount converted by the source; NULL when the source did not convert it")
    private BigDecimal baseAmount;

    @Column(name = "base_currency_code", comment = "Currency of the base amount")
    private String baseCurrencyCode;

    @Column(name = "note", comment = "Note of the record")
    private String note;

    @Column(name = "counterparty", comment = "Payee or payer")
    private String counterparty;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_state", nullable = false, comment = "State of the record in the source")
    private ExternalRecordState sourceState;

    @Column(name = "transfer_link_id", comment = "Id shared by both halves of a transfer between own accounts")
    private String transferLinkId;

    @Column(name = "source_updated_at", comment = "Last change of the record in the source")
    private Instant sourceUpdatedAt;

    @Column(name = "payload_hash", nullable = false, comment = "SHA-256 of the record content, detects changes")
    private String payloadHash;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", comment = "Representation of the record in the source")
    private String rawPayload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, comment = "Processing state of the record")
    private ImportRecordStatus status;

    @Column(name = "error_message", comment = "Why posting the record failed")
    private String errorMessage;

    @Column(name = "transaction_id", comment = "Accounting transaction the record was posted as")
    private Long transactionId;

    @Column(name = "posted_hash", comment = "Fingerprint of the transaction as posted; detects changes made in FRP")
    private String postedHash;

    @Column(name = "first_seen_at", nullable = false, updatable = false,
            comment = "First synchronization that fetched the record")
    private Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false, comment = "Last synchronization that fetched the record")
    private Instant lastSeenAt;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;
}
