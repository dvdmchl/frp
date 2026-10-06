package org.dreamabout.sw.frp.be.module.accounting.connector;

import java.util.Map;

/**
 * Credential values of a connection keyed by {@link CredentialField#name()}. {@link #toString()} hides the values.
 */
public record ConnectorCredentials(Map<String, String> values) {

    public ConnectorCredentials {
        values = Map.copyOf(values);
    }

    /**
     * Returns the value of the field; a missing or blank value means the connection cannot authenticate.
     */
    public String require(String name) {
        String value = values.get(name);
        if (value == null || value.isBlank()) {
            throw new ConnectorAuthException("Missing credential: " + name);
        }
        return value;
    }

    @Override
    public String toString() {
        return "ConnectorCredentials" + values.keySet();
    }
}
