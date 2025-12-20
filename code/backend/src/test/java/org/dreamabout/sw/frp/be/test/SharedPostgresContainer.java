package org.dreamabout.sw.frp.be.test;

import  org.testcontainers.postgresql.PostgreSQLContainer;

public class SharedPostgresContainer extends PostgreSQLContainer {

    public static final String POSTGRES_IMAGE_NAME = "postgres:17.1";
    private static final String DB_NAME = "db_frp_test";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "sa";


    private static final SharedPostgresContainer INSTANCE = new SharedPostgresContainer();

    private SharedPostgresContainer() {
        super(POSTGRES_IMAGE_NAME);
        withDatabaseName(DB_NAME);
        withUsername(DB_USER);
        withPassword(DB_PASSWORD);
        withReuse(true);
    }

    public static SharedPostgresContainer getInstance() {
        return INSTANCE;
    }
}
