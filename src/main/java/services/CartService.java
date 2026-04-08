package services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class CartService {
    private static final String CART_RECORD_CREATION_FAILED_MESSAGE = "Failed to create cart record.";
    private static final String INSERT_CART_RECORD = """
            INSERT INTO cart_records (total_items, total_cost, language)
            VALUES (?, ?, ?)
            """;

    private static final String INSERT_CART_ITEM = """
            INSERT INTO cart_items (cart_record_id, item_number, price, quantity, subtotal)
            VALUES (?, ?, ?, ?, ?)
            """;

    public void saveCart(int totalItems, double totalCost, String language, List<CartItem> items) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try {
                saveCartTransaction(connection, totalItems, totalCost, language, items);
            } catch (SQLException exception) {
                rollbackQuietly(connection, exception);
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    private void saveCartTransaction(Connection connection, int totalItems, double totalCost, String language,
                                     List<CartItem> items) throws SQLException {
        int cartRecordId = insertCartRecord(connection, totalItems, totalCost, language);
        insertCartItems(connection, cartRecordId, items);
        connection.commit();
    }

    private int insertCartRecord(Connection connection, int totalItems, double totalCost, String language)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_CART_RECORD, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, totalItems);
            statement.setDouble(2, totalCost);
            statement.setString(3, language);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        }

        throw new SQLException(CART_RECORD_CREATION_FAILED_MESSAGE);
    }

    private void insertCartItems(Connection connection, int cartRecordId, List<CartItem> items) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_CART_ITEM)) {
            statement.setInt(1, cartRecordId);
            for (CartItem item : items) {
                statement.setInt(2, item.itemNumber());
                statement.setDouble(3, item.price());
                statement.setInt(4, item.quantity());
                statement.setDouble(5, item.subtotal());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }
    private void rollbackQuietly(Connection connection, SQLException originalException) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            originalException.addSuppressed(rollbackException);
        }
    }
}
