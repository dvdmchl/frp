package org.dreamabout.sw.frp.be.module.accounting.connector.wallet;

import org.dreamabout.sw.frp.be.module.accounting.connector.ExternalAccount;
import org.dreamabout.sw.frp.be.module.accounting.connector.ExternalAmount;
import org.dreamabout.sw.frp.be.module.accounting.connector.ExternalCategory;
import org.dreamabout.sw.frp.be.module.accounting.connector.ExternalRecord;
import org.dreamabout.sw.frp.be.module.accounting.connector.ExternalRecordState;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Translates Wallet API entities into source-neutral ones.
 */
final class WalletMapper {

    private static final String CURRENCY_CODE = "currencyCode";

    private WalletMapper() {
    }

    static ExternalAccount toAccount(JsonNode account) {
        return new ExternalAccount(text(account, "id"), text(account, "name"), text(account, CURRENCY_CODE),
                account.toString());
    }

    static ExternalCategory toCategory(JsonNode category) {
        return new ExternalCategory(text(category, "id"), text(category, "name"), text(category, "parentId"),
                category.toString());
    }

    /**
     * Maps a record fetched with {@code convertTo=base}.
     *
     * @param zone zone in which the record date (a timestamp in Wallet) is a calendar day
     */
    static ExternalRecord toRecord(JsonNode walletRecord, ZoneId zone) {
        return ExternalRecord.builder()
                .externalId(text(walletRecord, "id"))
                .externalAccountId(text(walletRecord, "accountId"))
                .date(date(text(walletRecord, "recordDate"), zone))
                .amount(amount(walletRecord.path("amount")))
                .baseAmount(baseAmount(walletRecord.path("convertedAmount")))
                .externalCategoryId(text(walletRecord.path("category"), "id"))
                .note(text(walletRecord, "note"))
                .counterparty(text(walletRecord, "counterParty"))
                .state(state(text(walletRecord, "recordState")))
                .transferLinkId(transferLinkId(walletRecord.path("transfer")))
                .updatedAt(instant(text(walletRecord, "updatedAt")))
                .rawPayload(walletRecord.toString())
                .build();
    }

    private static LocalDate date(String recordDate, ZoneId zone) {
        return OffsetDateTime.parse(recordDate).atZoneSameInstant(zone).toLocalDate();
    }

    private static Instant instant(String timestamp) {
        return timestamp == null ? null : OffsetDateTime.parse(timestamp).toInstant();
    }

    private static ExternalAmount amount(JsonNode amount) {
        return new ExternalAmount(amount.path("value").decimalValue(), text(amount, CURRENCY_CODE));
    }

    /**
     * The amount converted to the base currency of the Wallet profile; {@code null} when Wallet has no rate for it.
     */
    private static ExternalAmount baseAmount(JsonNode convertedAmount) {
        if (!convertedAmount.path("value").isNumber() || text(convertedAmount, CURRENCY_CODE) == null) {
            return null;
        }
        return amount(convertedAmount);
    }

    private static ExternalRecordState state(String recordState) {
        if ("void".equals(recordState)) {
            return ExternalRecordState.DELETED;
        }
        return "uncleared".equals(recordState) ? ExternalRecordState.PENDING : ExternalRecordState.BOOKED;
    }

    /**
     * Only a paired transfer (both legs in own accounts) links two records; an unpaired one goes to or comes from an
     * account Wallet does not track and is a regular record.
     */
    private static String transferLinkId(JsonNode transfer) {
        return transfer.path("mirrorRecord").isObject() ? text(transfer, "transferId") : null;
    }

    /**
     * Text of the field; {@code null} when it is missing, not a text or blank.
     */
    private static String text(JsonNode node, String field) {
        var value = node.path(field);
        return value.isString() && !value.stringValue().isBlank() ? value.stringValue() : null;
    }
}
