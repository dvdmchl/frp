package org.dreamabout.sw.frp.be.module.accounting.connector;

import java.time.Duration;

/**
 * The source throttles requests; retry after {@link #retryAfter()}.
 */
public class ConnectorRateLimitedException extends ConnectorException {

    private final Duration retryAfter;

    public ConnectorRateLimitedException(String message, Duration retryAfter) {
        super(message);
        this.retryAfter = retryAfter;
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
