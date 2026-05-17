package org.classtrim.demo.ecommerce;

import java.util.HashMap;
import java.util.Map;

/**
 * Inventory tracks stock levels per SKU. Invoked by {@link OrderProcessor}.
 * Public instance methods are non-{@code @Override}, non-static,
 * non-constructor, with non-trivial bodies (analyzer-eligible).
 */
public class Inventory {

    Map<String, Integer> stockBySku = new HashMap<>();
    int lowStockThreshold;

    /** Reserves {@code qty} units of {@code sku}; returns success. */
    public boolean reserveStock(String sku, int qty) {
        if (sku == null || qty <= 0) {
            return false;
        }
        int current = stockBySku.getOrDefault(sku, 0);
        if (current < qty) {
            return false;
        }
        stockBySku.put(sku, current - qty);
        return true;
    }

    /** Returns {@code qty} units of {@code sku} to inventory. */
    public int releaseStock(String sku, int qty) {
        if (sku == null || qty <= 0) {
            return stockBySku.getOrDefault(sku, 0);
        }
        int current = stockBySku.getOrDefault(sku, 0);
        int updated = current + qty;
        stockBySku.put(sku, updated);
        return updated;
    }

    /** Surplus over the safety threshold; clamped at zero. */
    public int availableStock(String sku) {
        if (sku == null) {
            return 0;
        }
        int current = stockBySku.getOrDefault(sku, 0);
        int surplus = current - lowStockThreshold;
        if (surplus < 0) {
            return 0;
        }
        return surplus;
    }
}
