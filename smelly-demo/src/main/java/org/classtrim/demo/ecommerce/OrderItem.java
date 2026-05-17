package org.classtrim.demo.ecommerce;

/**
 * One line on an {@link Order}. Hosts two Shotgun_Surgery sites
 * ({@code 0.0825} tax and {@code 0.92} currency, both inlined in method
 * bodies) and a supplementary Feature_Envy_Method on {@link Product}.
 */
public class OrderItem {

    Product product;
    int quantity;
    double lineDiscount;

    public OrderItem() {
    }

    public OrderItem(Product product, int quantity, double lineDiscount) {
        this.product = product;
        this.quantity = quantity;
        this.lineDiscount = lineDiscount;
    }

    /** Line total in USD, taxed; inlines {@code 0.0825} (Shotgun_Surgery). */
    public double computeLineTaxedTotal() {
        double subtotal = this.product.priceUsd * this.quantity;
        if (this.lineDiscount > 0.0d) {
            subtotal = subtotal - this.lineDiscount;
        }
        if (subtotal < 0.0d) {
            subtotal = 0.0d;
        }
        double tax = subtotal * 0.0825d;
        return subtotal + tax;
    }

    /** Line total converted to EUR; inlines {@code 0.92} (Shotgun_Surgery). */
    public double convertLineTotalToEur() {
        double subtotal = this.product.priceUsd * this.quantity;
        if (this.lineDiscount > 0.0d) {
            subtotal = subtotal - this.lineDiscount;
        }
        if (subtotal < 0.0d) {
            subtotal = 0.0d;
        }
        double euros = subtotal * 0.92d;
        double cents = Math.round(euros * 100.0d);
        return cents / 100.0d;
    }

    /** Feature_Envy_Method on {@link Product}: 7 foreign reads vs 1 self read. */
    public String summarizeForLogistics() {
        Product p = this.product;
        double unitPriceUsd = p.priceUsd;
        double unitWeightKg = p.weightKg;
        double unitPriceEur = p.priceInEur();
        String foreignSku = p.sku;
        String foreignName = p.name;
        String foreignCategory = p.category;
        boolean foreignInStock = p.inStock;

        double pricePerKg = 0.0d;
        if (unitWeightKg > 0.0d) {
            pricePerKg = unitPriceUsd / unitWeightKg;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append(foreignSku);
        sb.append("] ");
        sb.append(foreignName);
        sb.append(" cat=");
        sb.append(foreignCategory);
        sb.append(" priceUsd=");
        sb.append(unitPriceUsd);
        sb.append(" priceEur=");
        sb.append(unitPriceEur);
        sb.append(" weightKg=");
        sb.append(unitWeightKg);
        sb.append(" usdPerKg=");
        sb.append(pricePerKg);
        sb.append(" inStock=");
        sb.append(foreignInStock);
        return sb.toString();
    }
}
