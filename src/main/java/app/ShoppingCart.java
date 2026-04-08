package app;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

import services.LocalizationService;

public class ShoppingCart {
    private static final Locale ENGLISH_LOCALE = Locale.of("en", "US");

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8)) {
            System.out.println("Select a language:");
            System.out.println("1. English");
            System.out.println("2. Finnish");
            System.out.println("3. Swedish");
            System.out.println("4. Japanese");
            System.out.println("5. Arabic");
            System.out.print("> ");
            int choice = scanner.nextInt();

            Locale locale = getLocaleForChoice(choice);
            Map<String, String> localizedStrings = LocalizationService.getLocalizedStrings(locale);

            System.out.println(localizedStrings.getOrDefault("prompt.items", "Enter number of items:"));
            int items = scanner.nextInt();

            double total = 0.0;
            for (int i = 1; i <= items; i++) {
                System.out.println(localizedStrings.getOrDefault("prompt.item_price", "Enter price for item") + " " + i + ":");
                double itemPrice = scanner.nextDouble();

                System.out.println(localizedStrings.getOrDefault("prompt.item_quantity", "Enter quantity for item") + " " + i + ":");
                double itemQuantity = scanner.nextDouble();

                double itemCost = calculateItemCost(itemPrice, itemQuantity);
                total = calculateTotal(total, itemCost);
            }
            System.out.println(localizedStrings.getOrDefault("result", "Total:") + " " + total);
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
}
