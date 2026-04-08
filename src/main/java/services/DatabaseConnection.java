package services;

import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {
    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_PORT = "3306";
    private static final String DEFAULT_DBNAME = "shopping_cart_localization";
    private static final String DEFAULT_USERNAME = "root";
    private static final String DEFAULT_PASSWORD = "password";
    private static final Dotenv DOTENV = Dotenv.configure()
            .ignoreIfMalformed()
            .ignoreIfMissing()
            .load();

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(getUrl(), getUsername(), getPassword());
    }

    private static String getUrl() {
        String dbHost = getConfigValue("DB_HOST", DEFAULT_HOST);
        String dbPort = getConfigValue("DB_PORT", DEFAULT_PORT);
        String dbName = getConfigValue("DB_NAME", DEFAULT_DBNAME);
        return "jdbc:mariadb://" + dbHost + ":" + dbPort + "/" + dbName
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    }

    private static String getUsername() {
        return getConfigValue("DB_USERNAME", DEFAULT_USERNAME);
    }

    private static String getPassword() {
        return getConfigValue("DB_PASSWORD", DEFAULT_PASSWORD);
    }

    private static String getConfigValue(String key, String defaultValue) {
        String dotenvValue = DOTENV.get(key);
        if (dotenvValue != null && !dotenvValue.isBlank()) {
            return dotenvValue;
        }

        return defaultValue;
    }
}
