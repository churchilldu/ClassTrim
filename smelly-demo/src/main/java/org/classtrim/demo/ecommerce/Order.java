package org.classtrim.demo.ecommerce;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregate domain class. Forward edge of the (Order, Customer) Inappropriate
 * Intimacy pair (matching back-edge on {@link Customer}). Hosts two
 * Feature_Envy_Methods on {@link Customer} and Shotgun_Surgery sites for
 * inlined tax ({@code 0.0825}) and currency ({@code 0.92}) literals.
 */
public class Order {

    String id;
    Customer customer;
    List<OrderItem> items = new ArrayList<>();
    String shippingAddressOverride;
    Instant placedAt;
    String currency;
    boolean paid;

    public Order() {
    }

    public Order(String id, Customer customer, List<OrderItem> items,
                 String shippingAddressOverride, Instant placedAt,
                 String currency, boolean paid) {
        this.id = id;
        this.customer = customer;
        this.items = items;
        this.shippingAddressOverride = shippingAddressOverride;
        this.placedAt = placedAt;
        this.currency = currency;
        this.paid = paid;
    }

    /** Feature_Envy_Method on {@link Customer}: 5 foreign reads vs 1 self read. */
    public String formatShippingLabel() {
        Customer c = this.customer;
        StringBuilder sb = new StringBuilder();
        sb.append("Ship to: ");
        sb.append(c.name);
        sb.append('\n');
        sb.append(c.street);
        sb.append('\n');
        sb.append(c.city);
        sb.append(", ");
        sb.append(c.postalCode);
        sb.append('\n');
        sb.append(c.country);
        return sb.toString();
    }

    /** Feature_Envy_Method on {@link Customer}: 3 foreign accesses vs 1 self read. */
    public double computeCustomerLifetimeValue() {
        Customer c = this.customer;
        int points = c.loyaltyPoints;
        String tier = c.vipTier;
        String evaluated = c.evaluateLoyaltyTier();
        double tierMultiplier;
        if ("PLATINUM".equals(tier) || (evaluated != null && evaluated.contains("PLATINUM"))) {
            tierMultiplier = 4.0d;
        } else if ("GOLD".equals(tier) || (evaluated != null && evaluated.contains("GOLD"))) {
            tierMultiplier = 2.5d;
        } else if ("SILVER".equals(tier) || (evaluated != null && evaluated.contains("SILVER"))) {
            tierMultiplier = 1.5d;
        } else {
            tierMultiplier = 1.0d;
        }
        return ((double) points) * tierMultiplier;
    }

    /** Shotgun_Surgery (tax). Inlines {@code 0.0825} directly. */
    public double computeOrderTotalWithTax() {
        List<OrderItem> lines = this.items;
        double subtotal = 0.0d;
        if (lines != null) {
            for (int i = 0; i < lines.size(); i++) {
                OrderItem line = lines.get(i);
                Product p = line.product;
                double unitPrice;
                if (p != null) {
                    unitPrice = p.priceUsd;
                } else {
                    unitPrice = 0.0d;
                }
                int qty = line.quantity;
                subtotal = subtotal + (unitPrice * ((double) qty));
            }
        }
        double tax = subtotal * 0.0825d;
        return subtotal + tax;
    }

    /** Shotgun_Surgery (currency). Inlines {@code 0.92} directly. */
    public double convertOrderTotalToEur() {
        List<OrderItem> lines = this.items;
        double totalUsd = 0.0d;
        if (lines != null) {
            for (int i = 0; i < lines.size(); i++) {
                OrderItem line = lines.get(i);
                Product p = line.product;
                double unitPrice;
                if (p != null) {
                    unitPrice = p.priceUsd;
                } else {
                    unitPrice = 0.0d;
                }
                int qty = line.quantity;
                totalUsd = totalUsd + (unitPrice * ((double) qty));
            }
        }
        double converted = totalUsd * 0.92d;
        if (converted < 0.0d) {
            converted = 0.0d;
        }
        return converted;
    }

    /** Marks the order paid; not a pure setter. */
    public void markPaid(Instant when) {
        if (!this.paid) {
            this.paid = true;
            if (this.placedAt == null) {
                this.placedAt = when;
            }
        }
    }

    /**
     * Closes the (Invoice, Order) Inappropriate Intimacy back-edge from the
     * {@code Order} side. Reads {@link Invoice#invoiceNumber},
     * {@link Invoice#lineItems}, and {@link Invoice#notes} directly without
     * holding a field reference to {@code Invoice}.
     */
    public String reconcileWithInvoice(Invoice invoice) {
        String number = invoice.invoiceNumber;
        List<OrderItem> mirroredLines = invoice.lineItems;
        String invoiceNotes = invoice.notes;

        int orderLineCount = 0;
        if (this.items != null) {
            orderLineCount = this.items.size();
        }
        int mirroredLineCount = 0;
        if (mirroredLines != null) {
            mirroredLineCount = mirroredLines.size();
        }
        int diff = orderLineCount - mirroredLineCount;
        if (diff < 0) {
            diff = -diff;
        }

        boolean hasNotes = invoiceNotes != null && invoiceNotes.length() > 0;

        StringBuilder sb = new StringBuilder();
        sb.append("reconcile[");
        sb.append(this.id);
        sb.append("] invoice=");
        sb.append(number);
        sb.append(" orderLines=");
        sb.append(orderLineCount);
        sb.append(" invoiceLines=");
        sb.append(mirroredLineCount);
        sb.append(" diff=");
        sb.append(diff);
        sb.append(" notes=");
        sb.append(hasNotes);
        return sb.toString();
    }
}
