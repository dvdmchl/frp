package org.dreamabout.sw.frp.be.module.accounting.connector.wallet;

import org.dreamabout.sw.frp.be.module.accounting.connector.AccountingConnector;
import org.dreamabout.sw.frp.be.module.accounting.connector.ConnectorCredentials;
import org.dreamabout.sw.frp.be.module.accounting.connector.CredentialField;
import org.dreamabout.sw.frp.be.module.accounting.connector.ExternalAccount;
import org.dreamabout.sw.frp.be.module.accounting.connector.ExternalCategory;
import org.dreamabout.sw.frp.be.module.accounting.connector.RecordPage;
import org.dreamabout.sw.frp.be.module.accounting.connector.RecordWindow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;
import tools.jackson.databind.JsonNode;

import java.net.URI;
import java.time.Clock;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Connector to the BudgetBakers Wallet REST API (needs Wallet Premium and a personal API token).
 *
 * <p>Wallet does not report deleted records (voided ones are {@link
 * org.dreamabout.sw.frp.be.module.accounting.connector.ExternalRecordState#DELETED}); the import pipeline marks
 * records of the window it no longer returns as deleted. Wallet allows about 300 requests per hour and token, so
 * pages are as large as the API allows.
 */
@Component
public class WalletConnector implements AccountingConnector {

    public static final String CONNECTOR_TYPE = "WALLET";
    public static final String API_TOKEN = "apiToken";

    static final int PAGE_SIZE = 200;
    private static final String ACCOUNTS = "accounts";
    private static final String CATEGORIES = "categories";
    private static final String RECORDS = "records";

    private static final String LIMIT = "limit";
    private static final String OFFSET = "offset";

    private final WalletApiClient client;
    private final ZoneId zone;

    @Autowired
    public WalletConnector(WalletConnectorProperties properties, Clock clock) {
        this(restClient(properties), clock.getZone());
    }

    WalletConnector(RestClient restClient, ZoneId zone) {
        this.client = new WalletApiClient(restClient);
        this.zone = zone;
    }

    private static RestClient restClient(WalletConnectorProperties properties) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getTimeout());
        requestFactory.setReadTimeout(properties.getTimeout());
        return RestClient.builder().baseUrl(properties.getBaseUrl()).requestFactory(requestFactory).build();
    }

    @Override
    public String type() {
        return CONNECTOR_TYPE;
    }

    @Override
    public List<CredentialField> credentialFields() {
        return List.of(CredentialField.secret(API_TOKEN));
    }

    @Override
    public void testConnection(ConnectorCredentials credentials) {
        probe(credentials);
    }

    /**
     * The {@code X-Last-Data-Change-Rev} Wallet sends with every response; it changes with any change of the data.
     */
    @Override
    public Optional<String> dataRevision(ConnectorCredentials credentials) {
        return Optional.ofNullable(probe(credentials).dataRevision());
    }

    @Override
    public List<ExternalAccount> fetchAccounts(ConnectorCredentials credentials) {
        return fetchAll(credentials, ACCOUNTS, WalletMapper::toAccount);
    }

    @Override
    public List<ExternalCategory> fetchCategories(ConnectorCredentials credentials) {
        return fetchAll(credentials, CATEGORIES, WalletMapper::toCategory);
    }

    /**
     * Fetches records with a record date in the window, oldest created first, with amounts converted to the base
     * currency of the Wallet profile. Wallet applies a 3-month window when no record date is given, so both bounds are
     * always sent. The cursor is the offset of the page.
     */
    @Override
    public RecordPage fetchRecords(ConnectorCredentials credentials, RecordWindow window, String cursor) {
        var offset = cursor == null ? 0 : Integer.parseInt(cursor);
        var body = client.get(credentials.require(API_TOKEN), uri -> recordsUri(uri, window, offset)).body();
        var records = body.path(RECORDS).values().stream().map(node -> WalletMapper.toRecord(node, zone)).toList();
        return nextOffset(body).map(next -> RecordPage.of(records, next)).orElseGet(() -> RecordPage.last(records));
    }

    private static URI recordsUri(UriBuilder uri, RecordWindow window, int offset) {
        Map<String, Object> values = new HashMap<>();
        values.put("from", "gte." + window.from());
        values.put("to", "lt." + window.to().plusDays(1));
        values.put("sortBy", "+createdAt");
        apiPath(uri, RECORDS)
                .queryParam("recordDate", "{from}", "{to}")
                .queryParam(LIMIT, PAGE_SIZE)
                .queryParam(OFFSET, offset)
                .queryParam("sortBy", "{sortBy}")
                .queryParam("convertTo", "base");
        window.updatedSince().ifPresent(since -> {
            uri.queryParam("updatedAt", "{updatedSince}");
            values.put("updatedSince", "gte." + since);
        });
        return uri.build(values);
    }

    private WalletApiClient.Response probe(ConnectorCredentials credentials) {
        return client.get(credentials.require(API_TOKEN),
                uri -> apiPath(uri, ACCOUNTS).queryParam(LIMIT, 1).build());
    }

    /**
     * Fetches all pages of a resource; the items of a page are in the field named like the resource.
     */
    private <T> List<T> fetchAll(ConnectorCredentials credentials, String resource, Function<JsonNode, T> mapper) {
        var apiToken = credentials.require(API_TOKEN);
        List<T> items = new ArrayList<>();
        Optional<String> offset = Optional.of("0");
        while (offset.isPresent()) {
            var pageOffset = offset.get();
            var body = client.get(apiToken, uri -> apiPath(uri, resource)
                    .queryParam(LIMIT, PAGE_SIZE)
                    .queryParam(OFFSET, pageOffset)
                    .build()).body();
            body.path(resource).values().forEach(item -> items.add(mapper.apply(item)));
            offset = nextOffset(body);
        }
        return List.copyOf(items);
    }

    private static UriBuilder apiPath(UriBuilder uri, String resource) {
        return uri.pathSegment("v1", "api", resource);
    }

    private static Optional<String> nextOffset(JsonNode page) {
        var next = page.path("nextOffset");
        return next.isIntegralNumber() ? Optional.of(next.asString()) : Optional.empty();
    }
}
