package app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class ShoppingCartApp extends Application {
    private static final String FXML_RESOURCE = "/shopping_cart.fxml";

    @Override
    public void start(Stage stage) throws IOException {
        URL resource = getClass().getResource(FXML_RESOURCE);
        if (resource == null) {
            throw new IOException("Resource not found: " + FXML_RESOURCE);
        }

        FXMLLoader loader = new FXMLLoader(resource);
        Scene scene = new Scene(loader.load(), 750, 500);

        stage.setTitle("NGOC NGUYEN / Shopping Cart App");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
