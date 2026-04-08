import app.ShoppingCart;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShoppingCartTest {

    @Test
    void calculateItemCost() {
        assertEquals(0.0, ShoppingCart.calculateItemCost(0.0, 5.0));
        assertEquals(10.0, ShoppingCart.calculateItemCost(5.0, 2.0));
    }

    @Test
    void calculateTotal() {
        assertEquals(0.0, ShoppingCart.calculateTotal(0.0, 0.0));
        assertEquals(10.0, ShoppingCart.calculateTotal(0.0, 10.0));
        assertEquals(15.0, ShoppingCart.calculateTotal(10.0, 5.0));
    }
}
