package com.stockbroker.validation;

import com.stockbroker.model.StockPrice;
import java.util.Locale;

public final class DemoInput {
    private DemoInput() {}

    public static String broker(String value) {
        if (value == null || !value.trim().matches("[A-Za-z0-9_-]{1,64}")) {
            throw new IllegalArgumentException("Broker ID must be 1-64 letters, digits, underscores or hyphens");
        }
        return value.trim();
    }

    public static String symbol(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z][A-Z0-9.-]{0,14}")) {
            throw new IllegalArgumentException("Stock symbol must be 1-15 ticker characters");
        }
        return normalized;
    }

    public static void price(StockPrice stock) {
        if (stock == null) throw new IllegalArgumentException("Price is required");
        stock.setSymbol(symbol(stock.getSymbol()));
        if (!Double.isFinite(stock.getCurrentPrice()) || stock.getCurrentPrice() <= 0
                || !Double.isFinite(stock.getChange()) || !Double.isFinite(stock.getChangePercent())
                || !Double.isFinite(stock.getVolume()) || stock.getVolume() < 0) {
            throw new IllegalArgumentException("Price must be positive; all numbers must be finite and volume nonnegative");
        }
    }
}
