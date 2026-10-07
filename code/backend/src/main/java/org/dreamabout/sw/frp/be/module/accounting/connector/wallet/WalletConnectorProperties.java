package org.dreamabout.sw.frp.be.module.accounting.connector.wallet;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "frp.connector.wallet")
@Getter
@Setter
public class WalletConnectorProperties {
    /**
     * Base URL of the BudgetBakers Wallet REST API.
     */
    private String baseUrl = "https://rest.budgetbakers.com/wallet";

    /**
     * Connect and read timeout of a request to the Wallet API.
     */
    private Duration timeout = Duration.ofSeconds(30);
}
