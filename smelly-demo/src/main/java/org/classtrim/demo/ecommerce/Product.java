package org.classtrim.demo.ecommerce;

/**
 * Catalog product. {@link #priceInEur()} inlines the {@code 0.92} USD-to-EUR
 * conversion literal directly in its body (Shotgun_Surgery site, no
 * static-final, no shared helper).
 */
public class Product {

    String sku;
    String name;
    double priceUsd;
    double weightKg;
    String category;
    boolean taxable;
    boolean inStock;

    public Product() {
    }

    public Product(String sku, String name, double priceUsd, double weightKg,
                   String category, boolean taxable, boolean inStock) {
        this.sku = sku;
        this.name = name;
        this.priceUsd = priceUsd;
        this.weightKg = weightKg;
        this.category = category;
        this.taxable = taxable;
        this.inStock = inStock;
    }

    /** Convert USD price to EUR using the inlined factor {@code 0.92}. */
    public double priceInEur() {
        if (!this.inStock) {
            return 0.0d;
        }
        double converted = this.priceUsd * 0.92d;
        if (converted < 0.0d) {
            converted = 0.0d;
        }
        double cents = Math.round(converted * 100.0d);
        return cents / 100.0d;
    }
}
