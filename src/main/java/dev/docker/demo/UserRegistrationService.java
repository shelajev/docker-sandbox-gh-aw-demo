package dev.docker.demo;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public final class UserRegistrationService {
    private static final String UNIQUE_VIOLATION = "23505";

    private final DataSource dataSource;

    public UserRegistrationService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void initializeSchema() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS users (
                        id BIGSERIAL PRIMARY KEY,
                        email VARCHAR(320) NOT NULL UNIQUE
                    )
                    """);
        }
    }

    public boolean register(String email) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             var statement = connection.prepareStatement(
                     "INSERT INTO users (email) VALUES (?)")) {
            statement.setString(1, email.toLowerCase());
            statement.executeUpdate();
            return true;
        } catch (SQLException exception) {
            if (UNIQUE_VIOLATION.equals(exception.getSQLState())) {
                return false;
            }
            throw exception;
        }
    }
}

