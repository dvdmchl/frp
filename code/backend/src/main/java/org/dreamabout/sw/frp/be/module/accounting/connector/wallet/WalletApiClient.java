package org.dreamabout.sw.frp.be.module.accounting.connector.wallet;

import org.dreamabout.sw.frp.be.module.accounting.connector.ConnectorAuthException;
import org.dreamabout.sw.frp.be.module.accounting.connector.ConnectorException;
import org.dreamabout.sw.frp.be.module.accounting.connector.ConnectorNotReadyException;
import org.dreamabout.sw.frp.be.module.accounting.connector.ConnectorRateLimitedException;
import org.dreamabout.sw.frp.be.module.accounting.connector.ConnectorTransientException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Function;

/**
 * HTTP access to the Wallet REST API: authenticates with the personal API token and translates error responses into
 * {@link ConnectorException}s.
 */
class WalletApiClient {

    static final String DATA_REVISION_HEADER = "X-Last-Data-Change-Rev";
    static final Duration DEFAULT_RETRY_AFTER = Duration.ofMinutes(5);

    private static final JsonMapper JSON = JsonMapper.builder()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .build();

    private final RestClient restClient;

    WalletApiClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Response body with the revision of the user's data in Wallet ({@code null} when the header is missing).
     */
    record Response(JsonNode body, String dataRevision) {
    }

    Response get(String apiToken, Function<UriBuilder, URI> uri) {
        try {
            return restClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange((request, response) -> read(response));
        } catch (RestClientException e) {
            throw new ConnectorTransientException("Wallet API is not reachable: " + e.getMessage(), e);
        }
    }

    private static Response read(ConvertibleClientHttpResponse response) throws IOException {
        var status = response.getStatusCode().value();
        var body = response.getBody().readAllBytes();
        if (response.getStatusCode().is2xxSuccessful()) {
            return new Response(JSON.readTree(body), response.getHeaders().getFirst(DATA_REVISION_HEADER));
        }
        var message = "Wallet API responded with HTTP " + status + ": " + errorOf(body);
        throw switch (status) {
            case 401, 403 -> new ConnectorAuthException(message);
            case 409 -> new ConnectorNotReadyException(message);
            case 429 -> new ConnectorRateLimitedException(message, retryAfter(response.getHeaders()));
            default -> new ConnectorTransientException(message, null);
        };
    }

    /**
     * The {@code error} of a JSON error body, otherwise the body as text (Wallet answers some errors in plain text).
     */
    private static String errorOf(byte[] body) {
        var text = new String(body, StandardCharsets.UTF_8).strip();
        try {
            var error = JSON.readTree(text).path("error");
            return error.isString() ? error.stringValue() : text;
        } catch (JacksonException _) {
            return text;
        }
    }

    private static Duration retryAfter(HttpHeaders headers) {
        var seconds = headers.getFirst(HttpHeaders.RETRY_AFTER);
        if (seconds == null || !seconds.strip().matches("\\d+")) {
            return DEFAULT_RETRY_AFTER;
        }
        return Duration.ofSeconds(Long.parseLong(seconds.strip()));
    }
}
