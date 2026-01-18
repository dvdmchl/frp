package org.dreamabout.sw.frp.be.module.accounting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dreamabout.sw.frp.be.module.accounting.config.AccountingProperties;
import org.dreamabout.sw.frp.be.module.common.service.SchemaCreationListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Currency;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountingSchemaListener implements SchemaCreationListener {

    private final AccountingProperties accountingProperties;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void onSchemaCreated(String schemaName, Long ownerId) {
        String baseCurrencyCode = accountingProperties.getBaseCurrency();
        if (baseCurrencyCode == null || baseCurrencyCode.isBlank()) {
            log.warn("No base currency configured in properties. Skipping base currency creation for schema: {}", schemaName);
            return;
        }

        try {
            Currency currency = Currency.getInstance(baseCurrencyCode);
            String currencyName = currency.getDisplayName();
            int defaultScale = 2; // Default scale for accounting

            log.info("Creating base currency {} ({}) for schema {}", baseCurrencyCode, currencyName, schemaName);

            String sql = """
                    INSERT INTO "%s".acc_currency (
                        id, code, name, is_base, scale, created_at, created_by_user_id, updated_at, updated_by_user_id, version
                    ) VALUES (
                        nextval('"%s".acc_currency_id_seq'), ?, ?, true, ?, ?, ?, ?, ?, 0
                    )
                    """.formatted(schemaName, schemaName);

            jdbcTemplate.update(sql,
                    baseCurrencyCode,
                    currencyName,
                    defaultScale,
                    LocalDateTime.now(),
                    ownerId,
                    LocalDateTime.now(),
                    ownerId
            );

        } catch (IllegalArgumentException e) {
            log.error("Invalid currency code configured: {}", baseCurrencyCode, e);
            throw new RuntimeException("Invalid base currency code: " + baseCurrencyCode, e);
        } catch (Exception e) {
            log.error("Failed to create base currency for schema {}", schemaName, e);
            throw new RuntimeException("Failed to create base currency", e);
        }
    }
}
