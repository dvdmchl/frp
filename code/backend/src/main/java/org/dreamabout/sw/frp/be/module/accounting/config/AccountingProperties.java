package org.dreamabout.sw.frp.be.module.accounting.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "frp.accounting")
@Getter
@Setter
public class AccountingProperties {
    /**
     * Default base currency code for new schemas.
     */
    private String baseCurrency;
}
