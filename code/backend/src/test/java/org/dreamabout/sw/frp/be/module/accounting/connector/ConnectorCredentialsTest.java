package org.dreamabout.sw.frp.be.module.accounting.connector;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConnectorCredentialsTest {

    @Test
    void shouldReturnRequiredValue() {
        var credentials = new ConnectorCredentials(Map.of("token", "secret-value"));

        assertThat(credentials.require("token")).isEqualTo("secret-value");
    }

    @Test
    void shouldFailWhenRequiredValueIsMissing() {
        var credentials = new ConnectorCredentials(Map.of());

        assertThatThrownBy(() -> credentials.require("token"))
                .isInstanceOf(ConnectorAuthException.class)
                .hasMessageContaining("token");
    }

    @Test
    void shouldFailWhenRequiredValueIsBlank() {
        var credentials = new ConnectorCredentials(Map.of("token", " "));

        assertThatThrownBy(() -> credentials.require("token"))
                .isInstanceOf(ConnectorAuthException.class);
    }

    @Test
    void shouldNotExposeValuesInToString() {
        var credentials = new ConnectorCredentials(Map.of("token", "secret-value"));

        assertThat(credentials.toString()).contains("token").doesNotContain("secret-value");
    }

    @Test
    void shouldNotReflectLaterChangesOfSourceMap() {
        var source = new HashMap<String, String>();
        source.put("token", "a");
        var credentials = new ConnectorCredentials(source);

        source.put("token", "b");

        assertThat(credentials.require("token")).isEqualTo("a");
    }
}
