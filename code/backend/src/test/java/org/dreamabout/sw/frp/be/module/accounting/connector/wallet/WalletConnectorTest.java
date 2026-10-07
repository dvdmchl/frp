package org.dreamabout.sw.frp.be.module.accounting.connector.wallet;

import org.dreamabout.sw.frp.be.module.accounting.connector.ConnectorAuthException;
import org.dreamabout.sw.frp.be.module.accounting.connector.ConnectorCredentials;
import org.dreamabout.sw.frp.be.module.accounting.connector.ConnectorNotReadyException;
import org.dreamabout.sw.frp.be.module.accounting.connector.ConnectorRateLimitedException;
import org.dreamabout.sw.frp.be.module.accounting.connector.ConnectorTransientException;
import org.dreamabout.sw.frp.be.module.accounting.connector.CredentialField;
import org.dreamabout.sw.frp.be.module.accounting.connector.ExternalAccount;
import org.dreamabout.sw.frp.be.module.accounting.connector.ExternalAmount;
import org.dreamabout.sw.frp.be.module.accounting.connector.ExternalCategory;
import org.dreamabout.sw.frp.be.module.accounting.connector.ExternalRecord;
import org.dreamabout.sw.frp.be.module.accounting.connector.ExternalRecordState;
import org.dreamabout.sw.frp.be.module.accounting.connector.RecordPage;
import org.dreamabout.sw.frp.be.module.accounting.connector.RecordWindow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WalletConnectorTest {

    private static final String BASE_URL = "https://wallet.test/wallet";
    private static final String TOKEN = "test-token";
    private static final ConnectorCredentials CREDENTIALS =
            new ConnectorCredentials(Map.of(WalletConnector.API_TOKEN, TOKEN));
    private static final ZoneId PRAGUE = ZoneId.of("Europe/Prague");
    private static final RecordWindow WINDOW =
            RecordWindow.between(LocalDate.of(2025, 11, 3), LocalDate.of(2026, 2, 1));
    private static final String PROBE_URL = BASE_URL + "/v1/api/accounts?limit=1";
    private static final String RECORDS_URL = BASE_URL + "/v1/api/records?recordDate=gte.2025-11-03"
            + "&recordDate=lt.2026-02-02&limit=200&offset=%d&sortBy=%%2BcreatedAt&convertTo=base";
    private static final String CEST_EXPENSE_ID = "550e8400-e29b-41d4-a716-446655440000";

    private MockRestServiceServer server;
    private WalletConnector connector;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        connector = new WalletConnector(builder.build(), PRAGUE);
    }

    @Test
    void shouldIdentifyItselfAsWalletWithSecretApiToken() {
        assertThat(connector.type()).isEqualTo("WALLET");
        assertThat(connector.credentialFields()).containsExactly(new CredentialField("apiToken", true));
    }

    @Test
    void shouldBeCreatedFromProperties() {
        var properties = new WalletConnectorProperties();

        var created = new WalletConnector(properties, Clock.systemUTC());

        assertThat(created.type()).isEqualTo(WalletConnector.CONNECTOR_TYPE);
        assertThat(properties.getBaseUrl()).isEqualTo("https://rest.budgetbakers.com/wallet");
        assertThat(properties.getTimeout()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void shouldSendApiTokenAsBearerTokenWhenTestingConnection() {
        expect(PROBE_URL).andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andRespond(withSuccess("{\"accounts\": []}", MediaType.APPLICATION_JSON));

        connector.testConnection(CREDENTIALS);

        server.verify();
    }

    @Test
    void shouldRejectMissingApiTokenWithoutCallingWallet() {
        server.expect(never(), requestTo(PROBE_URL));
        var empty = new ConnectorCredentials(Map.of());

        assertThatThrownBy(() -> connector.testConnection(empty)).isInstanceOf(ConnectorAuthException.class);
        server.verify();
    }

    @Test
    void shouldReportRejectedTokenAsAuthFailure() {
        expect(PROBE_URL).andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON).body("{\"error\": \"Invalid or expired token\"}"));

        assertThatThrownBy(() -> connector.testConnection(CREDENTIALS))
                .isInstanceOf(ConnectorAuthException.class)
                .hasMessage("Wallet API responded with HTTP 401: Invalid or expired token")
                .hasMessageNotContaining(TOKEN);
    }

    @Test
    void shouldReportTokenWithoutApiAccessAsAuthFailure() {
        expect(PROBE_URL).andRespond(withStatus(HttpStatus.FORBIDDEN).contentType(MediaType.TEXT_PLAIN)
                .body("Forbidden: this token belongs to the Board app"));

        assertThatThrownBy(() -> connector.testConnection(CREDENTIALS))
                .isInstanceOf(ConnectorAuthException.class)
                .hasMessageEndingWith("Forbidden: this token belongs to the Board app");
    }

    @Test
    void shouldReportInitialWalletSyncAsSourceNotReady() {
        expect(PROBE_URL).andRespond(withStatus(HttpStatus.CONFLICT));

        assertThatThrownBy(() -> connector.testConnection(CREDENTIALS))
                .isInstanceOf(ConnectorNotReadyException.class)
                .hasMessageContaining("409");
    }

    @Test
    void shouldReportRateLimitWithRetryAfterFromWallet() {
        expect(PROBE_URL).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, "120").contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\": \"Rate limit exceeded. Please try again later.\"}"));

        assertThatThrownBy(() -> connector.testConnection(CREDENTIALS))
                .isInstanceOfSatisfying(ConnectorRateLimitedException.class,
                        e -> assertThat(e.retryAfter()).isEqualTo(Duration.ofSeconds(120)))
                .hasMessageEndingWith("Rate limit exceeded. Please try again later.");
    }

    @Test
    void shouldUseDefaultRetryAfterWhenWalletDoesNotSendSeconds() {
        expect(PROBE_URL).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, "Wed, 21 Oct 2026 07:28:00 GMT"));

        assertThatThrownBy(() -> connector.testConnection(CREDENTIALS))
                .isInstanceOfSatisfying(ConnectorRateLimitedException.class,
                        e -> assertThat(e.retryAfter()).isEqualTo(WalletApiClient.DEFAULT_RETRY_AFTER));
    }

    @Test
    void shouldReportServerErrorAsTransient() {
        expect(PROBE_URL).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON).body("{\"error\": \"Internal Error\"}"));

        assertThatThrownBy(() -> connector.testConnection(CREDENTIALS))
                .isInstanceOf(ConnectorTransientException.class)
                .hasMessage("Wallet API responded with HTTP 500: Internal Error");
    }

    @Test
    void shouldReportNetworkFailureAsTransient() {
        expect(PROBE_URL).andRespond(withException(new IOException("Connection reset")));

        assertThatThrownBy(() -> connector.testConnection(CREDENTIALS))
                .isInstanceOf(ConnectorTransientException.class)
                .hasMessageContaining("not reachable");
    }

    @Test
    void shouldReturnDataRevisionOfWallet() {
        expect(PROBE_URL).andRespond(withSuccess("{\"accounts\": []}", MediaType.APPLICATION_JSON)
                .header(WalletApiClient.DATA_REVISION_HEADER, "r1234"));

        assertThat(connector.dataRevision(CREDENTIALS)).contains("r1234");
    }

    @Test
    void shouldReturnNoDataRevisionWhenHeaderIsMissing() {
        expect(PROBE_URL).andRespond(withSuccess("{\"accounts\": []}", MediaType.APPLICATION_JSON));

        assertThat(connector.dataRevision(CREDENTIALS)).isEmpty();
    }

    @Test
    void shouldFetchAccountsOfAllPages() {
        expect(BASE_URL + "/v1/api/accounts?limit=200&offset=0").andRespond(json("accounts-page-1.json"));
        expect(BASE_URL + "/v1/api/accounts?limit=200&offset=1").andRespond(json("accounts-page-2.json"));

        var accounts = connector.fetchAccounts(CREDENTIALS);

        assertThat(accounts)
                .extracting(ExternalAccount::externalId, ExternalAccount::name, ExternalAccount::currencyCode)
                .containsExactly(
                        tuple("6ba7b810-9dad-11d1-80b4-00c04fd430c8", "Current account",
                                "CZK"),
                        tuple("9f1c2d3e-4b5a-4c6d-8e7f-0a1b2c3d4e5f", "Euro cash",
                                "EUR"));
        assertThat(accounts.getFirst().rawPayload()).contains("\"accountType\":\"CurrentAccount\"");
        server.verify();
    }

    @Test
    void shouldFetchCategoriesWithTheirParent() {
        expect(BASE_URL + "/v1/api/categories?limit=200&offset=0").andRespond(json("categories.json"));

        var categories = connector.fetchCategories(CREDENTIALS);

        assertThat(categories)
                .extracting(ExternalCategory::externalId, ExternalCategory::name, ExternalCategory::parentExternalId)
                .containsExactly(
                        tuple("04f8dea1-33dc-411a-b2eb-b348c3168fa3", "Groceries", null),
                        tuple("c1d2e3f4-a5b6-4c7d-8e9f-001122334455", "Farmers market",
                                "04f8dea1-33dc-411a-b2eb-b348c3168fa3"));
    }

    @Test
    void shouldFetchRecordsOfWindowWithBaseAmountsAndCursorOfNextPage() {
        expect(RECORDS_URL.formatted(0)).andRespond(json("records-page-1.json"));

        var page = connector.fetchRecords(CREDENTIALS, WINDOW, null);

        assertThat(page.nextCursor()).isEqualTo("2");
        assertThat(page.records()).extracting(ExternalRecord::externalId)
                .containsExactly(CEST_EXPENSE_ID, "7c2b9d4e-1f3a-4b5c-8e6d-2a7f9b3c1e5a");
        server.verify();
    }

    @Test
    void shouldFetchNextPageFromCursorAndEndWithoutNextOffset() {
        expect(RECORDS_URL.formatted(2)).andRespond(json("records-page-2.json"));

        var page = connector.fetchRecords(CREDENTIALS, WINDOW, "2");

        assertThat(page.isLast()).isTrue();
        assertThat(page.records()).hasSize(3);
    }

    @Test
    void shouldFetchOnlyRecordsUpdatedSinceWhenWindowIsIncremental() {
        expect(RECORDS_URL.formatted(0) + "&updatedAt=gte.2026-01-31T06%3A00%3A00Z")
                .andRespond(withSuccess("{\"records\": [], \"offset\": 0}", MediaType.APPLICATION_JSON));

        var page = connector.fetchRecords(CREDENTIALS,
                WINDOW.withUpdatedSince(Instant.parse("2026-01-31T06:00:00Z")), null);

        assertThat(page.records()).isEmpty();
        assertThat(page.isLast()).isTrue();
        server.verify();
    }

    @Test
    void shouldMapRecordInForeignCurrencyWithAmountConvertedToBaseCurrency() {
        var expense = firstPageRecord(CEST_EXPENSE_ID);

        assertThat(expense)
                .returns("9f1c2d3e-4b5a-4c6d-8e7f-0a1b2c3d4e5f", ExternalRecord::externalAccountId)
                .returns(new ExternalAmount(new BigDecimal("-42.5"), "EUR"), ExternalRecord::amount)
                .returns(new ExternalAmount(new BigDecimal("-1062.5"), "CZK"), ExternalRecord::baseAmount)
                .returns("04f8dea1-33dc-411a-b2eb-b348c3168fa3", ExternalRecord::externalCategoryId)
                .returns("Weekly shopping", ExternalRecord::note)
                .returns("Billa", ExternalRecord::counterparty)
                .returns(ExternalRecordState.BOOKED, ExternalRecord::state)
                .returns(null, ExternalRecord::transferLinkId)
                .returns(Instant.parse("2026-01-15T08:05:00Z"), ExternalRecord::updatedAt);
        assertThat(expense.rawPayload()).contains("\"counterParty\":\"Billa\"");
    }

    @Test
    void shouldTakeRecordDateAsCalendarDayInZoneOfApplication() {
        assertThat(firstPageRecord(CEST_EXPENSE_ID).date()).isEqualTo(LocalDate.of(2026, 1, 15));
    }

    @Test
    void shouldTreatBlankNoteAsMissing() {
        assertThat(firstPageRecord("7c2b9d4e-1f3a-4b5c-8e6d-2a7f9b3c1e5a").note()).isNull();
    }

    @Test
    void shouldLinkLegsOfPairedTransfer() {
        assertThat(secondPageRecord("d0a1b2c3-0001-4000-8000-000000000001"))
                .returns("tr-42", ExternalRecord::transferLinkId)
                .returns(null, ExternalRecord::externalCategoryId);
    }

    @Test
    void shouldTreatUnpairedTransferAsPendingRegularRecord() {
        assertThat(secondPageRecord("d0a1b2c3-0001-4000-8000-000000000003"))
                .returns(null, ExternalRecord::transferLinkId)
                .returns(null, ExternalRecord::baseAmount)
                .returns(ExternalRecordState.PENDING, ExternalRecord::state);
    }

    @Test
    void shouldTreatVoidRecordAsDeletedAndSkipFailedConversion() {
        assertThat(secondPageRecord("d0a1b2c3-0001-4000-8000-000000000004"))
                .returns(ExternalRecordState.DELETED, ExternalRecord::state)
                .returns(null, ExternalRecord::baseAmount)
                .returns(new ExternalAmount(new BigDecimal("-3.2"), "EUR"), ExternalRecord::amount);
    }

    private ExternalRecord firstPageRecord(String externalId) {
        expect(RECORDS_URL.formatted(0)).andRespond(json("records-page-1.json"));
        return recordOf(connector.fetchRecords(CREDENTIALS, WINDOW, null), externalId);
    }

    private ExternalRecord secondPageRecord(String externalId) {
        expect(RECORDS_URL.formatted(2)).andRespond(json("records-page-2.json"));
        return recordOf(connector.fetchRecords(CREDENTIALS, WINDOW, "2"), externalId);
    }

    private static ExternalRecord recordOf(RecordPage page, String externalId) {
        return page.records().stream().filter(r -> r.externalId().equals(externalId)).findFirst().orElseThrow();
    }

    private ResponseActions expect(String url) {
        return server.expect(requestTo(url)).andExpect(method(HttpMethod.GET));
    }

    private static ResponseCreator json(String fixture) {
        return withSuccess(new ClassPathResource("wallet/" + fixture), MediaType.APPLICATION_JSON);
    }
}
