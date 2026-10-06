package org.dreamabout.sw.frp.be.module.accounting.connector;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * In-memory connector used to exercise the SPI in tests. Serves one record per page.
 */
class FakeAccountingConnector implements AccountingConnector {

    static final String TOKEN = "token";
    static final String VALID_TOKEN = "valid-token";

    private final String type;
    private final List<ExternalRecord> records;

    FakeAccountingConnector(String type, List<ExternalRecord> records) {
        this.type = type;
        this.records = List.copyOf(records);
    }

    static ExternalRecord bookedRecord(String externalId, String amount) {
        return ExternalRecord.builder()
                .externalId(externalId)
                .externalAccountId("acc-1")
                .date(LocalDate.of(2026, 1, 15))
                .amount(new ExternalAmount(new BigDecimal(amount), "CZK"))
                .state(ExternalRecordState.BOOKED)
                .updatedAt(Instant.parse("2026-01-15T10:00:00Z"))
                .rawPayload("{}")
                .build();
    }

    @Override
    public String type() {
        return type;
    }

    @Override
    public List<CredentialField> credentialFields() {
        return List.of(CredentialField.secret(TOKEN));
    }

    @Override
    public void testConnection(ConnectorCredentials credentials) {
        if (!VALID_TOKEN.equals(credentials.require(TOKEN))) {
            throw new ConnectorAuthException("Invalid token");
        }
    }

    @Override
    public List<ExternalAccount> fetchAccounts(ConnectorCredentials credentials) {
        testConnection(credentials);
        return List.of(new ExternalAccount("acc-1", "Cash", "CZK", "{}"));
    }

    @Override
    public List<ExternalCategory> fetchCategories(ConnectorCredentials credentials) {
        testConnection(credentials);
        return List.of(new ExternalCategory("cat-1", "Food", null, "{}"));
    }

    @Override
    public RecordPage fetchRecords(ConnectorCredentials credentials, RecordWindow window, String cursor) {
        testConnection(credentials);
        int index = cursor == null ? 0 : Integer.parseInt(cursor);
        List<ExternalRecord> page = List.of(records.get(index));
        int next = index + 1;
        return next < records.size() ? RecordPage.of(page, String.valueOf(next)) : RecordPage.last(page);
    }
}
