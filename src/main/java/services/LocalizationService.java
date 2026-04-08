package services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class LocalizationService {
    private static final Logger LOGGER = Logger.getLogger(LocalizationService.class.getName());
    private static final String SELECT_LOCALIZED_STRINGS = """
            SELECT `key`, value
            FROM localization_strings
            WHERE language = ?
            """;

    private LocalizationService() {
    }

    /**
     * Get localized strings for a specific locale
     */
    public static Map<String, String> getLocalizedStrings(Locale locale) {
        Map<String, String> strings = new LinkedHashMap<>();
        String languageCode = toLanguageCode(locale);

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_LOCALIZED_STRINGS)) {
            statement.setString(1, languageCode);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    strings.put(resultSet.getString("key"), resultSet.getString("value"));
                }
            }
        } catch (SQLException exception) {
            LOGGER.log(Level.WARNING, "Failed to load localized strings from database.", exception);
        }

        if (strings.isEmpty() && !"en_US".equals(languageCode)) {
            strings.putAll(getLocalizedStrings(Locale.of("en", "US")));
        }

        if (strings.isEmpty()) {
            strings.putAll(getDefaultEnglishStrings());
        }

        return strings;
    }

    public static String toLanguageCode(Locale locale) {
        return locale.getLanguage() + "_" + locale.getCountry();
    }

    private static Map<String, String> getDefaultEnglishStrings() {
        Map<String, String> defaults = new LinkedHashMap<>();
        defaults.put("label.language", "Select the language:");
        defaults.put("prompt.items", "Enter number of items:");
        defaults.put("prompt.item_price", "Enter price for item");
        defaults.put("prompt.item_quantity", "Enter quantity for item");
        defaults.put("result", "Total:");
        defaults.put("button.generate", "Enter items");
        defaults.put("button.calculate", "Calculate Total");
        defaults.put("error.title", "Error");
        defaults.put("error.invalid_number", "Please enter a valid number of items.");
        defaults.put("error.invalid_input", "Please enter valid price and quantity values.");
        defaults.put("error.database", "Database operation failed. Please check your database connection.");
        defaults.put("message.saved", "Cart saved successfully.");
        return defaults;
    }
}
