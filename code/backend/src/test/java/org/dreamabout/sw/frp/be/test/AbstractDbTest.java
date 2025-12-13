package org.dreamabout.sw.frp.be.test;

import jakarta.persistence.Table;
import org.dreamabout.sw.frp.be.domain.Constant;
import org.dreamabout.sw.frp.be.module.common.model.IdAwareEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.platform.commons.util.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = Replace.NONE)
@AutoConfigureMockMvc
public abstract class AbstractDbTest {

    private static final String TRUNCATE_SCRIPT = """
            DO $$
            DECLARE
                schema_name text;
                table_name text;
            BEGIN
                FOR schema_name, table_name IN
                    SELECT schemaname, tablename
                    FROM pg_tables
                    WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
                      AND tablename NOT IN ('flyway_schema_history')
                LOOP
                    EXECUTE format('TRUNCATE TABLE %I.%I RESTART IDENTITY CASCADE', schema_name, table_name);
                END LOOP;
            END $$;
            """;

    static final SharedPostgresContainer POSTGRES_CONTAINER = SharedPostgresContainer.getInstance();

    static {
        POSTGRES_CONTAINER.start();
    }

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);

        registry.add("JWT_SECRET_KEY", () -> "test-secret-key-123");
    }

    @BeforeEach
    void truncateAll() {
        jdbcTemplate.execute(TRUNCATE_SCRIPT);
    }

    protected <T extends IdAwareEntity> List<T> selectAllFromPublicSchema(Class<T> clazz) {
        return selectAll(clazz, Constant.PUBLIC_SCHEMA);
    }

    protected <T extends IdAwareEntity> List<T> selectAll(Class<T> clazz, String schema) {
        Preconditions.notNull(clazz, "Class cannot be null");
        var tableName = getTableName(clazz);
        var sql = String.format("SELECT * FROM %s.%s", schema, tableName);
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(clazz));
    }

    /**
     * Return table name from an entity class
     */
    protected String getTableName(Class<?> clazz) {
        Preconditions.notNull(clazz, "Class cannot be null");
        var tableAnnotation = clazz.getAnnotation(Table.class);
        if (tableAnnotation != null) {
            return tableAnnotation.name();
        }
        throw new IllegalArgumentException(String.format("Class %s is not annotated with @Table", clazz.getName()));
    }
}
