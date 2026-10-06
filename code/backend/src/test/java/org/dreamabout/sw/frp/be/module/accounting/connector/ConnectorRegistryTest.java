package org.dreamabout.sw.frp.be.module.accounting.connector;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConnectorRegistryTest {

    private final FakeAccountingConnector wallet = new FakeAccountingConnector("WALLET", List.of());
    private final FakeAccountingConnector bank = new FakeAccountingConnector("BANK", List.of());

    @Test
    void shouldReturnConnectorByType() {
        var registry = new ConnectorRegistry(List.of(wallet, bank));

        assertThat(registry.get("WALLET")).isSameAs(wallet);
        assertThat(registry.get("BANK")).isSameAs(bank);
    }

    @Test
    void shouldFailFastWhenTypeIsUnknown() {
        var registry = new ConnectorRegistry(List.of(wallet));

        assertThatThrownBy(() -> registry.get("BANK"))
                .isInstanceOf(UnknownConnectorTypeException.class)
                .hasMessageContaining("BANK");
    }

    @Test
    void shouldRejectTwoConnectorsWithTheSameType() {
        var duplicate = new FakeAccountingConnector("WALLET", List.of());
        List<AccountingConnector> connectors = List.of(wallet, duplicate);

        assertThatThrownBy(() -> new ConnectorRegistry(connectors))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("WALLET");
    }

    @Test
    void shouldListRegisteredTypesSorted() {
        var registry = new ConnectorRegistry(List.of(wallet, bank));

        assertThat(registry.types()).containsExactly("BANK", "WALLET");
    }
}
