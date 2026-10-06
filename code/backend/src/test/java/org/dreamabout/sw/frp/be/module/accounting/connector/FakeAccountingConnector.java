package org.dreamabout.sw.frp.be.module.accounting.connector;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * In-memory connector used to exercise the SPI in tests. Serves one record per page; the records and a hook run
 * before every page fetch can be replaced between syncs.
 */
public class FakeAccountingConnector implements AccountingConnector {

    public static final String TOKEN = "token";
    public static final String VALID_TOKEN = "valid-token";

    private final String type;
    private final List<String> requestedCursors = Collections.synchronizedList(new ArrayList<>());
    private volatile List<ExternalRecord> records;
    private volatile Consumer<String> beforeFetch = cursor -> { };

    public FakeAccountingConnector(String type, List<ExternalRecord> records) {
        this.type = type;
        this.records = List.copyOf(records);
    }

    public static ExternalRecord bookedRecord(String externalId, String amount) {
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

    public void setRecords(List<ExternalRecord> records) {
        this.records = List.copyOf(records);
    }

    /**
     * Runs the hook with the requested cursor before a page is served; it may throw to simulate a failing source.
     */
    public void setBeforeFetch(Consumer<String> beforeFetch) {
        this.beforeFetch = beforeFetch;
    }

    /**
     * Cursors of all page requests in order; {@code null} stands for the first page.
     */
    public List<String> requestedCursors() {
        synchronized (requestedCursors) {
            return Collections.unmodifiableList(new ArrayList<>(requestedCursors));
        }
    }

    public void reset() {
        records = List.of();
        beforeFetch = cursor -> { };
        requestedCursors.clear();
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
        requestedCursors.add(cursor);
        beforeFetch.accept(cursor);
        List<ExternalRecord> served = records;
        if (served.isEmpty()) {
            return RecordPage.last(List.of());
        }
        int index = cursor == null ? 0 : Integer.parseInt(cursor);
        List<ExternalRecord> page = List.of(served.get(index));
        int next = index + 1;
        return next < served.size() ? RecordPage.of(page, String.valueOf(next)) : RecordPage.last(page);
    }
}
