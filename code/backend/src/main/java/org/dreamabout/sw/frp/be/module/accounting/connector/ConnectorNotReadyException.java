package org.dreamabout.sw.frp.be.module.accounting.connector;

/**
 * The source cannot serve data yet (e.g. it is still running its own initial synchronization); retry later.
 */
public class ConnectorNotReadyException extends ConnectorException {

    public ConnectorNotReadyException(String message) {
        super(message);
    }
}
