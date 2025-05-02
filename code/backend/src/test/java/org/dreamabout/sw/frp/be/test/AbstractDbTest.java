package org.dreamabout.sw.frp.be.test;

import jakarta.persistence.Table;
import org.dreamabout.sw.frp.be.domain.Constant;
import org.dreamabout.sw.frp.be.model.IdAwareEntity;
import org.junit.platform.commons.util.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = Replace.NONE)
public abstract class AbstractDbTest {

    private static final String DB_NAME = "db_frp_test";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "sa";

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Container
    private static final PostgreSQLContainer POSTGRES_CONTAINER = new PostgreSQLContainer("postgres:" + Constant.POSTGRES_VERSION)
            .withDatabaseName(DB_NAME)
            .withUsername(DB_USER)
            .withPassword(DB_PASSWORD);

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
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
     * Return table name from entity class
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
