package org.classtrim.demo.ecommerce;

import java.time.Instant;
import java.util.List;

/**
 * Hosts the (Invoice, Order) Inappropriate Intimacy back-edge, a
 * Feature_Envy_Method on {@link Order}, and two Shotgun_Surgery sites
 * (inlined {@code 0.0825} tax, {@code 0.92} currency).
 */
public class Invoice {

    String invoiceNumber;
    Order order;
    Instant issuedAt;
    List<OrderItem> lineItems;
    String notes;

    public Invoice() {
    }

    public Invoice(String invoiceNumber, Order order, Instant issuedAt,
                   List<OrderItem> lineItems, String notes) {
        this.invoiceNumber = invoiceNumber;
        this.order = order;
        this.issuedAt = issuedAt;
        this.lineItems = lineItems;
        this.notes = notes;
    }

    /** Feature_Envy_Method on {@link Order}: 4 foreign reads vs 1 self read. */
    public String renderInvoiceLines() {
        Order o = this.order;
        String orderId = o.id;
        Customer cust = o.customer;
        List<OrderItem> orderItems = o.items;
        Instant when = o.placedAt;

        String custName = null;
        if (cust != null) {
            custName = cust.name;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Invoice for order ");
        sb.append(orderId);
        sb.append(" placed at ");
        sb.append(when);
        sb.append(" for ");
        sb.append(custName);
        sb.append('\n');

        if (orderItems != null) {
            for (int i = 0; i < orderItems.size(); i++) {
                OrderItem line = orderItems.get(i);
                Product prod = line.product;
                String prodName = null;
                if (prod != null) {
                    prodName = prod.name;
                }
                int qty = line.quantity;
                sb.append("  ");
                sb.append(qty);
                sb.append(" x ");
                sb.append(prodName);
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    /** Shotgun_Surgery (tax). Inlines {@code 0.0825} directly. */
    public double computeInvoiceGrandTotalWithTax() {
        List<OrderItem> lines = this.lineItems;
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
                double lineSubtotal = unitPrice * ((double) qty);
                if (line.lineDiscount > 0.0d) {
                    lineSubtotal = lineSubtotal - line.lineDiscount;
                }
                if (lineSubtotal < 0.0d) {
                    lineSubtotal = 0.0d;
                }
                subtotal = subtotal + lineSubtotal;
            }
        }
        double tax = subtotal * 0.0825d;
        return subtotal + tax;
    }

    /** Shotgun_Surgery (currency). Inlines {@code 0.92} directly. */
    public double convertGrandTotalToEur() {
        List<OrderItem> lines = this.lineItems;
        double subtotalUsd = 0.0d;
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
                subtotalUsd = subtotalUsd + (unitPrice * ((double) qty));
            }
        }
        double euros = subtotalUsd * 0.92d;
        if (euros < 0.0d) {
            euros = 0.0d;
        }
        double cents = Math.round(euros * 100.0d);
        return cents / 100.0d;
    }

    /** Closes the (Invoice, Order) Inappropriate Intimacy back-edge. */
    public String auditOrderItems() {
        Order o = this.order;
        List<OrderItem> orderItems = o.items;
        String orderId = o.id;
        boolean orderPaid = o.paid;

        int totalQty = 0;
        int suspiciousLines = 0;
        if (orderItems != null) {
            for (int i = 0; i < orderItems.size(); i++) {
                OrderItem line = orderItems.get(i);
                int q = line.quantity;
                totalQty = totalQty + q;
                if (q > 100) {
                    suspiciousLines = suspiciousLines + 1;
                }
            }
        }

        int mirroredCount = 0;
        if (this.lineItems != null) {
            mirroredCount = this.lineItems.size();
        }
        int orderItemCount = 0;
        if (orderItems != null) {
            orderItemCount = orderItems.size();
        }
        int mismatch = orderItemCount - mirroredCount;
        if (mismatch < 0) {
            mismatch = -mismatch;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("audit[");
        sb.append(orderId);
        sb.append("] invoice=");
        sb.append(this.invoiceNumber);
        sb.append(" totalQty=");
        sb.append(totalQty);
        sb.append(" suspicious=");
        sb.append(suspiciousLines);
        sb.append(" mismatch=");
        sb.append(mismatch);
        sb.append(" paid=");
        sb.append(orderPaid);
        return sb.toString();
    }
}
