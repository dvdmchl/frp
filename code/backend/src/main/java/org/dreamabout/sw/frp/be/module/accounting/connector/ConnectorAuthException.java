package org.dreamabout.sw.frp.be.module.accounting.connector;

/**
 * The source rejected the credentials, or they are incomplete. Retrying will not help until the user fixes them.
 */
public class ConnectorAuthException extends ConnectorException {

    public ConnectorAuthException(String message) {
        super(message);
    }
}
