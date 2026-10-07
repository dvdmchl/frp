package org.dreamabout.sw.frp.be.module.accounting.connector;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountingConnectorTest {

    private static final ConnectorCredentials VALID =
            new ConnectorCredentials(Map.of(FakeAccountingConnector.TOKEN, FakeAccountingConnector.VALID_TOKEN));
    private static final RecordWindow JANUARY =
            RecordWindow.between(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

    private final AccountingConnector connector = new FakeAccountingConnector("FAKE", List.of(
            FakeAccountingConnector.bookedRecord("r1", "-100.50"),
            FakeAccountingConnector.bookedRecord("r2", "2000"),
            FakeAccountingConnector.bookedRecord("r3", "-15")));

    @Test
    void shouldFetchAllRecordsByFollowingCursor() {
        List<String> ids = new ArrayList<>();
        RecordPage page = connector.fetchRecords(VALID, JANUARY, null);
        page.records().forEach(r -> ids.add(r.externalId()));
        while (!page.isLast()) {
            page = connector.fetchRecords(VALID, JANUARY, page.nextCursor());
            page.records().forEach(r -> ids.add(r.externalId()));
        }

        assertThat(ids).containsExactly("r1", "r2", "r3");
    }

    @Test
    void shouldDescribeCredentialFields() {
        assertThat(connector.credentialFields())
                .containsExactly(new CredentialField(FakeAccountingConnector.TOKEN, true));
    }

    @Test
    void shouldReportAuthFailureOnInvalidCredentials() {
        var invalid = new ConnectorCredentials(Map.of(FakeAccountingConnector.TOKEN, "wrong"));

        assertThatThrownBy(() -> connector.testConnection(invalid))
                .isInstanceOf(ConnectorAuthException.class)
                .isInstanceOf(ConnectorException.class);
    }

    @Test
    void shouldFetchAccountsAndCategories() {
        assertThat(connector.fetchAccounts(VALID)).extracting(ExternalAccount::externalId).containsExactly("acc-1");
        assertThat(connector.fetchCategories(VALID)).extracting(ExternalCategory::externalId).containsExactly("cat-1");
    }

    @Test
    void shouldReportNoDataRevisionWhenSourceDoesNotTellIt() {
        assertThat(connector.dataRevision(VALID)).isEmpty();
    }

    @Test
    void shouldCarryRetryAfterWhenRateLimited() {
        var exception = new ConnectorRateLimitedException("Too many requests", Duration.ofMinutes(5));

        assertThat(exception.retryAfter()).isEqualTo(Duration.ofMinutes(5));
        assertThat(exception).isInstanceOf(ConnectorException.class);
    }

    @Test
    void shouldKeepCauseOfTransientError() {
        var cause = new IOException("connection reset");

        var exception = new ConnectorTransientException("Source unavailable", cause);

        assertThat(exception).hasCause(cause).isInstanceOf(ConnectorException.class);
    }

    @Test
    void shouldSignalSourceNotReady() {
        assertThat(new ConnectorNotReadyException("Initial sync running")).isInstanceOf(ConnectorException.class);
    }
}
