package dev.docker.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class UserRegistrationServiceTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName
                    .parse("postgres:16.6-alpine3.21@sha256:1d04b9ba1d4996401f2552b51beda8187f175c0645c091e4781134fc9c9a3eef")
                    .asCompatibleSubstituteFor("postgres"));

    private UserRegistrationService registrations;

    @BeforeEach
    void resetDatabase() throws SQLException {
        var dataSource = new PGSimpleDataSource();
        dataSource.setURL(POSTGRES.getJdbcUrl());
        dataSource.setUser(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());

        try (var connection = dataSource.getConnection();
             var statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE IF EXISTS users");
        }

        registrations = new UserRegistrationService(dataSource);
        registrations.initializeSchema();
    }

    @Test
    void rejectsAnExactDuplicateEmailAddress() throws SQLException {
        assertTrue(registrations.register("alice@example.com"));
        assertFalse(registrations.register("alice@example.com"));
    }

    @Test
    void rejectsCaseVariantEmailAddress() throws SQLException {
        assertTrue(registrations.register("bob@example.com"));
        assertFalse(registrations.register("BOB@example.com"));
    }
}
