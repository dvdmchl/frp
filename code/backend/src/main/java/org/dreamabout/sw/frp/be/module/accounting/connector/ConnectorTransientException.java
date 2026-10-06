package org.dreamabout.sw.frp.be.module.accounting.connector;

/**
 * Temporary failure (network error, 5xx response); the same request may succeed later.
 */
public class ConnectorTransientException extends ConnectorException {

    public ConnectorTransientException(String message, Throwable cause) {
        super(message, cause);
    }
}
