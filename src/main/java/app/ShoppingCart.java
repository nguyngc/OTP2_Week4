package app;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Logger;

import services.LocalizationService;

public class ShoppingCart {
    private static final Locale ENGLISH_LOCALE = Locale.of("en", "US");
    private static final Logger LOGGER = Logger.getLogger(ShoppingCart.class.getName());

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8)) {
            log("Select a language:");
            log("1. English");
            log("2. Finnish");
            log("3. Swedish");
            log("4. Japanese");
            log("5. Arabic");
            log("> ");
            int choice = scanner.nextInt();

            Locale locale = getLocaleForChoice(choice);
            Map<String, String> localizedStrings = LocalizationService.getLocalizedStrings(locale);

            log(localizedStrings.getOrDefault("prompt.items", "Enter number of items:"));
            int items = scanner.nextInt();

            double total = 0.0;
            for (int i = 1; i <= items; i++) {
                log(localizedStrings.getOrDefault("prompt.item_price", "Enter price for item") + " " + i + ":");
                double itemPrice = scanner.nextDouble();

                log(localizedStrings.getOrDefault("prompt.item_quantity", "Enter quantity for item") + " " + i + ":");
                double itemQuantity = scanner.nextDouble();

                double itemCost = calculateItemCost(itemPrice, itemQuantity);
                total = calculateTotal(total, itemCost);
            }
            log(localizedStrings.getOrDefault("result", "Total:") + " " + total);
        }
    }

    public static double calculateItemCost(double itemPrice, double itemQuantity) {
        return itemPrice * itemQuantity;
    }

    public static double calculateTotal(double currentTotal, double itemCost) {
        return currentTotal + itemCost;
    }

    private static Locale getLocaleForChoice(int choice) {
        return switch (choice) {
            case 2 -> Locale.of("fi", "FI");
            case 3 -> Locale.of("sv", "SE");
            case 4 -> Locale.of("ja", "JP");
            case 5 -> Locale.of("ar", "AR");
            case 1 -> ENGLISH_LOCALE;
            default -> ENGLISH_LOCALE;
        };
    }

    private static void log(String message) {
        LOGGER.info(message);
    }
}
