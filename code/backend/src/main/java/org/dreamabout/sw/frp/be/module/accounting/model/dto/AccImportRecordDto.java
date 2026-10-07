package org.dreamabout.sw.frp.be.module.accounting.model.dto;

import org.dreamabout.sw.frp.be.module.accounting.connector.ExternalRecordState;
import org.dreamabout.sw.frp.be.module.accounting.domain.ImportRecordStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Record staged from an external source and the state of its posting.
 *
 * @param amount         signed amount; negative = money going out of the account
 * @param transferLinkId id shared by both halves of a transfer between own accounts
 * @param errorMessage   why the record failed or is in conflict
 * @param transactionId  accounting transaction the record was posted as
 */
public record AccImportRecordDto(
    Long id,
    String externalId,
    String externalAccountId,
    String externalCategoryId,
    LocalDate recordDate,
    BigDecimal amount,
    String currencyCode,
    String note,
    String counterparty,
    ExternalRecordState sourceState,
    String transferLinkId,
    ImportRecordStatus status,
    String errorMessage,
    Long transactionId,
    Instant lastSeenAt
) {}
