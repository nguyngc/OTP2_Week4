package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import services.CartItem;
import services.CartService;
import services.LocalizationService;

import java.text.NumberFormat;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import java.util.Map;

public class ShoppingCartController {
    private static final Locale ENGLISH_LOCALE = Locale.of("en", "US");
    private static final String LANGUAGE_ENGLISH = "English";
    private static final String LANGUAGE_FINNISH = "Finnish";
    private static final String LANGUAGE_SWEDISH = "Swedish";
    private static final String LANGUAGE_JAPANESE = "Japanese";
    private static final String LANGUAGE_ARABIC = "Arabic";
    private static final String RESULT_KEY = "result";
    private static final String DEFAULT_TOTAL_LABEL = "Total";
    private static final String DEFAULT_LANGUAGE_LABEL = "Select the language:";
    private static final String DEFAULT_ITEMS_PROMPT = "Enter number of items:";
    private static final String DEFAULT_ITEM_PRICE_PROMPT = "Enter price for item";
    private static final String DEFAULT_ITEM_QUANTITY_PROMPT = "Enter quantity for item";
    private static final String DEFAULT_CALCULATE_LABEL = "Calculate " + DEFAULT_TOTAL_LABEL;
    private static final String DEFAULT_TOTAL_RESULT = DEFAULT_TOTAL_LABEL + ":";
    private static final String DEFAULT_INVALID_NUMBER_ERROR = "Please enter a valid number of items.";
    private static final String DEFAULT_INVALID_INPUT_ERROR = "Please enter valid price and quantity values.";
    private static final String DEFAULT_DATABASE_ERROR =
            "Database operation failed. Please check your database connection.";
    private static final String DEFAULT_ERROR_TITLE = "Error";
    private static final CartService CART_SERVICE = new CartService();

    @FXML
    private VBox rootVBox;

    @FXML
    private Label lblLanguage;

    @FXML
    private ComboBox<String> cbLanguage;

    @FXML
    private Label lblItemCount;

    @FXML
    private TextField txtItemCount;

    @FXML
    private Button btnGenerate;

    @FXML
    private Button btnCalculate;

    @FXML
    private VBox itemsContainer;

    @FXML
    private Label lblTotal;

    private Locale currentLocale = ENGLISH_LOCALE;
    private Map<String, String> localizedStrings;

    @FXML
    public void initialize() {
        setLanguage(currentLocale);

        cbLanguage.getItems().addAll(
                LANGUAGE_ENGLISH,
                LANGUAGE_FINNISH,
                LANGUAGE_SWEDISH,
                LANGUAGE_JAPANESE,
                LANGUAGE_ARABIC
        );
        cbLanguage.setValue(LANGUAGE_ENGLISH);

        cbLanguage.setOnAction(e -> {
            txtItemCount.clear();
            currentLocale = getLocaleForLanguage(cbLanguage.getValue());
            setLanguage(currentLocale);
        });
    }

    /**
     * Set the application language
     */
    private void setLanguage(Locale locale) {
        lblTotal.setText("");
        itemsContainer.getChildren().clear();

        localizedStrings = LocalizationService.getLocalizedStrings(locale);
        updateTexts();
        applyTextDirection(locale);
    }

    private void updateTexts() {
        lblLanguage.setText(localizedStrings.getOrDefault("label.language", DEFAULT_LANGUAGE_LABEL));
        lblItemCount.setText(localizedStrings.getOrDefault("prompt.items", DEFAULT_ITEMS_PROMPT));
        btnGenerate.setText(localizedStrings.getOrDefault("button.generate", "Enter items"));
        btnCalculate.setText(localizedStrings.getOrDefault("button.calculate", DEFAULT_CALCULATE_LABEL));
        lblTotal.setText(localizedStrings.getOrDefault(RESULT_KEY, DEFAULT_TOTAL_RESULT) + " " + formatCurrency(0.0));
        txtItemCount.setPromptText(localizedStrings.getOrDefault("prompt.items", DEFAULT_ITEMS_PROMPT));
    }

    @FXML
    private void handleGenerate() {
        itemsContainer.getChildren().clear();

        int itemCount;
        try {
            itemCount = Integer.parseInt(txtItemCount.getText().trim());
            if (itemCount <= 0) {
                showAlert(localizedStrings.getOrDefault("error.invalid_number", DEFAULT_INVALID_NUMBER_ERROR));
                return;
            }
        } catch (NumberFormatException exception) {
            showAlert(localizedStrings.getOrDefault("error.invalid_number", DEFAULT_INVALID_NUMBER_ERROR));
            return;
        }

        for (int i = 1; i <= itemCount; i++) {
            itemsContainer.getChildren().add(createItemRow(i));
        }
    }

    private HBox createItemRow(int index) {
        String itemPricePrompt = localizedStrings.getOrDefault("prompt.item_price", DEFAULT_ITEM_PRICE_PROMPT);
        String itemQuantityPrompt = localizedStrings.getOrDefault("prompt.item_quantity", DEFAULT_ITEM_QUANTITY_PROMPT);

        Label lblPrice = new Label(itemPricePrompt + " " + index + ":");
        TextField txtPrice = new TextField();
        txtPrice.setPromptText(itemPricePrompt + " " + index);
        txtPrice.setPrefWidth(100);

        Label lblQuantity = new Label(itemQuantityPrompt + " " + index + ":");
        TextField txtQuantity = new TextField();
        txtQuantity.setPromptText(itemQuantityPrompt + " " + index);
        txtQuantity.setPrefWidth(100);

        Label lblItemCost = new Label(localizedStrings.getOrDefault(RESULT_KEY, DEFAULT_TOTAL_LABEL) + ": " + formatCurrency(0.0));
        lblItemCost.setPrefWidth(140);

        HBox row = new HBox(10, lblPrice, txtPrice, lblQuantity, txtQuantity, lblItemCost);
        row.setUserData(new ItemRowData(txtPrice, txtQuantity, lblItemCost, index));
        return row;
    }

    @FXML
    private void handleCalculate() {
        double total = 0.0;
        List<CartItem> cartItems = new ArrayList<>();

        try {
            for (var node : itemsContainer.getChildren()) {
                if (node instanceof HBox row && row.getUserData() instanceof ItemRowData data) {
                    double price = Double.parseDouble(data.txtPrice.getText().trim());
                    int quantity = Integer.parseInt(data.txtQuantity.getText().trim());

                    double itemTotal = calculateItemCost(price, quantity);
                    total += itemTotal;
                    cartItems.add(new CartItem(data.index, price, quantity, itemTotal));

                    data.lblTotal.setText(localizedStrings.getOrDefault(RESULT_KEY, DEFAULT_TOTAL_RESULT) + " " + formatCurrency(itemTotal));
                }
            }

            lblTotal.setText(localizedStrings.getOrDefault(RESULT_KEY, DEFAULT_TOTAL_RESULT) + " " + formatCurrency(total));
            saveCart(cartItems, total);

        } catch (NumberFormatException exception) {
            showAlert(localizedStrings.getOrDefault("error.invalid_input", DEFAULT_INVALID_INPUT_ERROR));
        }
    }

    private void saveCart(List<CartItem> cartItems, double total) {
        try {
            CART_SERVICE.saveCart(cartItems.size(), total, LocalizationService.toLanguageCode(currentLocale), cartItems);
        } catch (SQLException exception) {
            showAlert(localizedStrings.getOrDefault(
                    "error.database",
                    DEFAULT_DATABASE_ERROR
            ));
        }
    }

    private String formatCurrency(double amount) {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(getCurrencyLocale());
        return currencyFormat.format(amount);
    }

    private Locale getCurrencyLocale() {
        if ("ar".equals(currentLocale.getLanguage())) {
            return Locale.of("ar", "AR");
        }
        return currentLocale;
    }

    /**
     * Apply LTR or RTL layout direction
     */
    private void applyTextDirection(Locale locale) {
        String lang = locale.getLanguage();
        boolean isRTL = "fa".equals(lang)
                || "ur".equals(lang)
                || "ar".equals(lang)
                || "he".equals(lang);

        Platform.runLater(() -> {
            if (rootVBox != null) {
                rootVBox.setNodeOrientation(
                        isRTL ? NodeOrientation.RIGHT_TO_LEFT
                                : NodeOrientation.LEFT_TO_RIGHT
                );
            }
        });
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(localizedStrings.getOrDefault("error.title", DEFAULT_ERROR_TITLE));
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static double calculateItemCost(double itemPrice, double itemQuantity) {
        return itemPrice * itemQuantity;
    }

    private Locale getLocaleForLanguage(String language) {
        if (language == null) {
            return ENGLISH_LOCALE;
        }

        return switch (language) {
            case LANGUAGE_FINNISH -> Locale.of("fi", "FI");
            case LANGUAGE_SWEDISH -> Locale.of("sv", "SE");
            case LANGUAGE_JAPANESE -> Locale.of("ja", "JP");
            case LANGUAGE_ARABIC -> Locale.of("ar", "AR");
            case LANGUAGE_ENGLISH -> ENGLISH_LOCALE;
            default -> ENGLISH_LOCALE;
        };
    }

    private static class ItemRowData {
        private final TextField txtPrice;
        private final TextField txtQuantity;
        private final Label lblTotal;
        private final int index;

        ItemRowData(TextField txtPrice, TextField txtQuantity, Label lblTotal, int index) {
            this.txtPrice = txtPrice;
            this.txtQuantity = txtQuantity;
            this.lblTotal = lblTotal;
            this.index = index;
        }
    }
}
