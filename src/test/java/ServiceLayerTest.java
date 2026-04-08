import app.ShoppingCart;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import services.CartItem;
import services.CartService;
import services.DatabaseConnection;
import services.LocalizationService;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class ServiceLayerTest {
    private static final String INSERT_CART_RECORD = """
            INSERT INTO cart_records (total_items, total_cost, language)
            VALUES (?, ?, ?)
            """;

    private static final String INSERT_CART_ITEM = """
            INSERT INTO cart_items (cart_record_id, item_number, price, quantity, subtotal)
            VALUES (?, ?, ?, ?, ?)
            """;

    @AfterEach
    void tearDown() throws SQLException {
        FakeJdbcDriver.uninstall();
    }

    @Test
    void databaseConnectionUsesDefaultConfiguration() throws Exception {
        FakeJdbcDriver.ConnectionState state = new FakeJdbcDriver.ConnectionState();
        FakeJdbcDriver.install((url, info) -> captureConnection(state, url, info));

        try (Connection ignored = DatabaseConnection.getConnection()) {
            assertTrue(state.capturedUrl.startsWith("jdbc:mariadb://"));
            assertTrue(state.capturedUrl.contains("?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"));
            assertNotNull(state.capturedUser);
            assertFalse(state.capturedUser.isBlank());
            assertNotNull(state.capturedPassword);
            assertFalse(state.capturedPassword.isBlank());
        }
    }

    @Test
    void localizationServiceReturnsDatabaseRowsAndFallbacks() throws Exception {
        FakeJdbcDriver.ConnectionState state = new FakeJdbcDriver.ConnectionState();
        FakeJdbcDriver.PreparedStatementState selectState = new FakeJdbcDriver.PreparedStatementState();
        selectState.queryRowsProvider = parameters -> switch ((String) parameters.get(1)) {
            case "fi_FI" -> List.of();
            case "en_US" -> List.of(
                    Map.of("key", "label.language", "value", "Choose language"),
                    Map.of("key", "result", "value", "Total:")
            );
            default -> List.of(Map.of("key", "label.language", "value", "Localized"));
        };
        state.statements.put("""
            SELECT `key`, value
            FROM localization_strings
            WHERE language = ?
            """, selectState);

        FakeJdbcDriver.install((url, info) -> captureConnection(state, url, info));

        Map<String, String> finnish = LocalizationService.getLocalizedStrings(Locale.of("fi", "FI"));
        Map<String, String> japanese = LocalizationService.getLocalizedStrings(Locale.of("ja", "JP"));

        assertEquals("Choose language", finnish.get("label.language"));
        assertEquals("Localized", japanese.get("label.language"));
        assertEquals("ja_JP", LocalizationService.toLanguageCode(Locale.of("ja", "JP")));
    }

    @Test
    void localizationServiceUsesDefaultStringsWhenDatabaseFails() throws Exception {
        FakeJdbcDriver.install((url, info) -> {
            throw new SQLException("Database unavailable");
        });

        Map<String, String> strings = LocalizationService.getLocalizedStrings(Locale.of("fi", "FI"));

        assertEquals("Select the language:", strings.get("label.language"));
        assertEquals("Cart saved successfully.", strings.get("message.saved"));
    }

    @Test
    void cartServiceCommitsSuccessfulTransaction() throws Exception {
        FakeJdbcDriver.ConnectionState state = new FakeJdbcDriver.ConnectionState();

        FakeJdbcDriver.PreparedStatementState recordInsert = new FakeJdbcDriver.PreparedStatementState();
        recordInsert.generatedKeyRows = List.of(Map.of(1, 101));
        state.statements.put(INSERT_CART_RECORD, recordInsert);

        FakeJdbcDriver.PreparedStatementState itemInsert = new FakeJdbcDriver.PreparedStatementState();
        state.statements.put(INSERT_CART_ITEM, itemInsert);

        FakeJdbcDriver.install((url, info) -> captureConnection(state, url, info));

        CartService service = new CartService();
        List<CartItem> items = List.of(
                new CartItem(1, 2.5, 2, 5.0),
                new CartItem(2, 3.0, 1, 3.0)
        );

        service.saveCart(2, 8.0, "en_US", items);

        assertTrue(state.autoCommit);
        assertEquals(1, state.commitCount);
        assertEquals(0, state.rollbackCount);
        assertEquals(2, itemInsert.batches.size());
        assertEquals(101, itemInsert.batches.get(0).get(1));
        assertEquals(1, itemInsert.batches.get(0).get(2));
        assertEquals(2, itemInsert.batches.get(0).get(4));
        assertTrue(recordInsert.executeUpdateCalled);
        assertTrue(itemInsert.executeBatchCalled);
    }

    @Test
    void cartServiceRollsBackWhenBatchInsertFails() throws Exception {
        FakeJdbcDriver.ConnectionState state = new FakeJdbcDriver.ConnectionState();

        FakeJdbcDriver.PreparedStatementState recordInsert = new FakeJdbcDriver.PreparedStatementState();
        recordInsert.generatedKeyRows = List.of(Map.of(1, 88));
        state.statements.put(INSERT_CART_RECORD, recordInsert);

        FakeJdbcDriver.PreparedStatementState itemInsert = new FakeJdbcDriver.PreparedStatementState();
        itemInsert.executeBatchException = new SQLException("Batch failed");
        state.statements.put(INSERT_CART_ITEM, itemInsert);

        FakeJdbcDriver.install((url, info) -> captureConnection(state, url, info));

        CartService service = new CartService();

        SQLException exception = assertThrows(SQLException.class, () ->
                service.saveCart(1, 5.0, "en_US", List.of(new CartItem(1, 5.0, 1, 5.0)))
        );

        assertEquals("Batch failed", exception.getMessage());
        assertEquals(1, state.rollbackCount);
        assertEquals(0, state.commitCount);
    }

    @Test
    void shoppingCartMainPrintsCalculatedTotal() throws Exception {
        FakeJdbcDriver.ConnectionState state = new FakeJdbcDriver.ConnectionState();
        FakeJdbcDriver.PreparedStatementState selectState = new FakeJdbcDriver.PreparedStatementState();
        selectState.queryRowsProvider = parameters -> List.of(
                Map.of("key", "prompt.items", "value", "Enter number of items:"),
                Map.of("key", "prompt.item_price", "value", "Enter price for item"),
                Map.of("key", "prompt.item_quantity", "value", "Enter quantity for item"),
                Map.of("key", "result", "value", "Total:")
        );
        state.statements.put("""
            SELECT `key`, value
            FROM localization_strings
            WHERE language = ?
            """, selectState);
        FakeJdbcDriver.install((url, info) -> captureConnection(state, url, info));

        var originalIn = System.in;
        var input = new ByteArrayInputStream("1\n2\n5\n2\n3\n1\n".getBytes(StandardCharsets.UTF_8));
        var logger = Logger.getLogger(ShoppingCart.class.getName());
        var originalUseParentHandlers = logger.getUseParentHandlers();
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.INFO);
        var messages = new StringBuilder();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord logRecord) {
                messages.append(logRecord.getMessage()).append('\n');
            }

            @Override
            public void flush() {
                // Nothing to flush because the test handler only buffers messages in memory.
            }

            @Override
            public void close() {
                // Nothing to close because the test handler does not own external resources.
            }
        };
        logger.addHandler(handler);

        try {
            System.setIn(input);
            ShoppingCart.main(new String[0]);
        } finally {
            System.setIn(originalIn);
            logger.removeHandler(handler);
            logger.setUseParentHandlers(originalUseParentHandlers);
        }

        String printed = messages.toString();
        assertTrue(printed.contains("Select a language:"));
        assertTrue(printed.contains("Total: 13.0"));
    }

    @Test
    void shoppingCartMainSupportsAdditionalLanguageSelections() throws Exception {
        FakeJdbcDriver.ConnectionState state = new FakeJdbcDriver.ConnectionState();
        FakeJdbcDriver.PreparedStatementState selectState = new FakeJdbcDriver.PreparedStatementState();
        List<String> requestedLanguages = new java.util.ArrayList<>();
        selectState.queryRowsProvider = parameters -> {
            requestedLanguages.add((String) parameters.get(1));
            return List.of(
                    Map.of("key", "prompt.items", "value", "Enter number of items:"),
                    Map.of("key", "result", "value", "Total:")
            );
        };
        state.statements.put("""
            SELECT `key`, value
            FROM localization_strings
            WHERE language = ?
            """, selectState);
        FakeJdbcDriver.install((url, info) -> captureConnection(state, url, info));

        runShoppingCartMain("2\n0\n");
        runShoppingCartMain("3\n0\n");
        runShoppingCartMain("4\n0\n");
        runShoppingCartMain("5\n0\n");
        runShoppingCartMain("9\n0\n");

        assertTrue(requestedLanguages.containsAll(Set.of("fi_FI", "sv_SE", "ja_JP", "ar_AR", "en_US")));
    }

    private Connection captureConnection(FakeJdbcDriver.ConnectionState state, String url, Properties info) {
        state.capturedUrl = url;
        state.capturedUser = info.getProperty("user");
        state.capturedPassword = info.getProperty("password");
        return state.toConnection();
    }

    private void runShoppingCartMain(String inputText) throws Exception {
        var originalIn = System.in;
        var logger = Logger.getLogger(ShoppingCart.class.getName());
        var originalUseParentHandlers = logger.getUseParentHandlers();
        logger.setUseParentHandlers(false);
        try (ByteArrayInputStream testInput = new ByteArrayInputStream(inputText.getBytes(StandardCharsets.UTF_8));
             NoOpLogHandler handler = new NoOpLogHandler()) {
            logger.addHandler(handler);
            System.setIn(testInput);
            ShoppingCart.main(new String[0]);
        } finally {
            System.setIn(originalIn);
            for (Handler handler : logger.getHandlers()) {
                if (handler instanceof NoOpLogHandler) {
                    logger.removeHandler(handler);
                }
            }
            logger.setUseParentHandlers(originalUseParentHandlers);
        }
    }

    private static final class NoOpLogHandler extends Handler implements AutoCloseable {
        @Override
        public void publish(LogRecord logRecord) {
            // Intentionally ignore log records because this helper only prevents test log noise.
        }

        @Override
        public void flush() {
            // Nothing to flush because the test handler only suppresses log output.
        }

        @Override
        public void close() {
            // Nothing to close because the test handler does not own external resources.
        }
    }
}
