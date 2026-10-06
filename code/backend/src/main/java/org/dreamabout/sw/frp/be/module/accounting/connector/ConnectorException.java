package org.dreamabout.sw.frp.be.module.accounting.connector;

/**
 * Failure of a connector while talking to its source. Subtypes tell the import pipeline how to react.
 */
public abstract class ConnectorException extends RuntimeException {

    protected ConnectorException(String message) {
        super(message);
    }

    protected ConnectorException(String message, Throwable cause) {
        super(message, cause);
    }
}
