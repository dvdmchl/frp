package org.dreamabout.sw.frp.be.config.db;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
@Slf4j
public class FlywayDevConfig {

    @Bean
    public FlywayMigrationStrategy cleanMigrateStrategy() {
        return flyway -> {
            log.warn("DEV PROFILE: Dropping and recreating database with Flyway (clean + migrate).");
            flyway.clean();
            flyway.migrate();
        };
    }
}
