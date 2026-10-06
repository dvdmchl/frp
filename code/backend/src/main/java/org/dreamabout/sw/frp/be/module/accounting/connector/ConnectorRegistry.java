package org.dreamabout.sw.frp.be.module.accounting.connector;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * All {@link AccountingConnector} beans by {@link AccountingConnector#type()}.
 * Two connectors with the same type fail the application start.
 */
@Component
public final class ConnectorRegistry {

    private final Map<String, AccountingConnector> connectorsByType;

    public ConnectorRegistry(List<AccountingConnector> connectors) {
        Map<String, AccountingConnector> byType = new TreeMap<>();
        for (AccountingConnector connector : connectors) {
            AccountingConnector previous = byType.putIfAbsent(connector.type(), connector);
            if (previous != null) {
                throw new IllegalStateException("Duplicate accounting connector type " + connector.type() + ": "
                        + previous.getClass().getName() + " and " + connector.getClass().getName());
            }
        }
        this.connectorsByType = Collections.unmodifiableMap(byType);
    }

    public AccountingConnector get(String type) {
        AccountingConnector connector = connectorsByType.get(type);
        if (connector == null) {
            throw new UnknownConnectorTypeException(type);
        }
        return connector;
    }

    public SortedSet<String> types() {
        return Collections.unmodifiableSortedSet(new TreeSet<>(connectorsByType.keySet()));
    }
}
