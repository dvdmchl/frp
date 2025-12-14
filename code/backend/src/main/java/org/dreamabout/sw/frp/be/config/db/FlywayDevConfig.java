package org.dreamabout.sw.frp.be.config.db;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
@Slf4j
public class FlywayDevConfig {

    @Value("${frp.flyway.clean:false}")
    private boolean clean;

    @Bean
    public FlywayMigrationStrategy cleanMigrateStrategy() {
        return flyway -> {
            if (clean) {
                log.warn("DEV PROFILE: Dropping and recreating database with Flyway (clean + migrate).");
                flyway.clean();
            } else {
                log.info("DEV PROFILE: Migrating database with Flyway (clean skipped).");
            }
            flyway.migrate();
        };
    }
}
