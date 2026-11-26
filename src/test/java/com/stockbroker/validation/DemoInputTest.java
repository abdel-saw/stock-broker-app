package com.stockbroker.validation;

import com.stockbroker.model.StockPrice;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DemoInputTest {
    @Test
    void boundsBrokerAndSymbolInputs() {
        for (String broker : new String[]{"<script>", "with space", "x".repeat(65), "a/b"}) {
            assertThrows(IllegalArgumentException.class, () -> DemoInput.broker(broker));
        }
        for (String symbol : new String[]{"<img>", "A".repeat(16), "A/B", "A B"}) {
            assertThrows(IllegalArgumentException.class, () -> DemoInput.symbol(symbol));
        }
        assertEquals("BRK.B", DemoInput.symbol(" brk.b "));
        assertEquals("demo-1_test", DemoInput.broker(" demo-1_test "));
    }

    @Test
    void rejectsNonfiniteChangeAndVolume() {
        var stock = new StockPrice("AAPL", 10);
        stock.setChange(Double.NaN);
        assertThrows(IllegalArgumentException.class, () -> DemoInput.price(stock));
        stock.setChange(0);
        stock.setChangePercent(Double.POSITIVE_INFINITY);
        assertThrows(IllegalArgumentException.class, () -> DemoInput.price(stock));
        stock.setChangePercent(0);
        stock.setVolume(Double.NaN);
        assertThrows(IllegalArgumentException.class, () -> DemoInput.price(stock));
    }
}
