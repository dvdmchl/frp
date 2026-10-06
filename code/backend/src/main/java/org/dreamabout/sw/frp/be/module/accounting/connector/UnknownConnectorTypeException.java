package org.dreamabout.sw.frp.be.module.accounting.connector;

/**
 * No connector is registered for the requested type.
 */
public class UnknownConnectorTypeException extends IllegalArgumentException {

    public UnknownConnectorTypeException(String type) {
        super("Unknown connector type: " + type);
    }
}
