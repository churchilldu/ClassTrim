package org.classtrim.demo.ecommerce;

import java.util.ArrayList;
import java.util.List;

/**
 * God Class for the smelly e-commerce fixture. Orchestrates orders by reaching
 * into seven sibling Demo_Classes ({@link Order}, {@link OrderItem},
 * {@link Customer}, {@link Product}, {@link Inventory},
 * {@link ShippingCalculator}, {@link Invoice}) across many public, non-static,
 * non-{@code @Override} instance methods. Three of those methods are
 * Feature_Envy_Methods on a single foreign Demo_Class, two duplicate
 * Shotgun_Surgery literals ({@code 0.0825} for tax, {@code 0.92} for currency)
 * directly in their bodies.
 */
public class OrderProcessor {

    Inventory inventory;
    ShippingCalculator shippingCalculator;
    Invoice currentInvoice;
    String defaultCurrency;
    List<String> auditTrail = new ArrayList<>();

    public OrderProcessor() {
    }

    public OrderProcessor(Inventory inventory,
                          ShippingCalculator shippingCalculator,
                          String defaultCurrency) {
        this.inventory = inventory;
        this.shippingCalculator = shippingCalculator;
        this.defaultCurrency = defaultCurrency;
    }

    /**
     * Top-level orchestrator that reserves stock, charges the customer, and
     * dispatches shipment for an incoming order. Touches Inventory,
     * ShippingCalculator, Order, OrderItem, Product, and Customer.
     */
    public boolean processNewOrder(Order o) {
        if (o == null) {
            return false;
        }
        boolean reserved = this.reserveInventoryFor(o);
        if (!reserved) {
            this.auditTrail.add("processNewOrder: reservation failed for " + o.id);
            return false;
        }
        boolean charged = this.chargeCustomer(o);
        if (!charged) {
            this.releaseInventoryFor(o);
            this.auditTrail.add("processNewOrder: charge failed for " + o.id);
            return false;
        }
        double shipping = this.shippingCalculator.calculateShippingForOrder(o);
        this.dispatchShipmentFor(o);
        this.generateInvoiceFor(o);
        this.auditTrail.add("processNewOrder: complete for " + o.id + " shipping=" + shipping);
        return true;
    }

    /**
     * Feature_Envy_Method on {@link Customer}. The {@code customer} reference
     * is bound once via a single foreign read on {@code Order}; thereafter all
     * data flows from the {@code Customer} object. Foreign Customer accesses
     * (five) strictly exceed self-class accesses on {@code OrderProcessor}.
     */
    public boolean chargeCustomer(Order o) {
        Customer c = o.customer;
        String custEmail = c.email;
        int points = c.loyaltyPoints;
        String tier = c.vipTier;
        String country = c.country;
        String evaluated = c.evaluateLoyaltyTier();
        boolean approved;
        if (custEmail == null || custEmail.isEmpty()) {
            approved = false;
        } else if (points < 0) {
            approved = false;
        } else if ("BLOCKED".equals(tier)) {
            approved = false;
        } else if (country != null && country.startsWith("XX")) {
            approved = false;
        } else {
            approved = true;
        }
        this.auditTrail.add("chargeCustomer: " + custEmail + " tier=" + evaluated + " approved=" + approved);
        return approved;
    }

    /** Reserves stock for every line in the order via {@link Inventory}. */
    public boolean reserveInventoryFor(Order o) {
        List<OrderItem> lines = o.items;
        if (lines == null) {
            return true;
        }
        List<String> reserved = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            OrderItem line = lines.get(i);
            Product p = line.product;
            if (p == null) {
                continue;
            }
            String sku = p.sku;
            int qty = line.quantity;
            boolean ok = this.inventory.reserveStock(sku, qty);
            if (!ok) {
                for (int j = 0; j < reserved.size(); j++) {
                    String prevSku = reserved.get(j);
                    OrderItem prevLine = lines.get(j);
                    this.inventory.releaseStock(prevSku, prevLine.quantity);
                }
                this.auditTrail.add("reserveInventoryFor: failed at sku=" + sku);
                return false;
            }
            reserved.add(sku);
        }
        this.auditTrail.add("reserveInventoryFor: reserved " + reserved.size() + " line(s)");
        return true;
    }

    /** Returns reserved stock for every line via {@link Inventory}. */
    public void releaseInventoryFor(Order o) {
        List<OrderItem> lines = o.items;
        if (lines == null) {
            return;
        }
        for (int i = 0; i < lines.size(); i++) {
            OrderItem line = lines.get(i);
            Product p = line.product;
            if (p == null) {
                continue;
            }
            String sku = p.sku;
            int qty = line.quantity;
            this.inventory.releaseStock(sku, qty);
        }
        this.auditTrail.add("releaseInventoryFor: released " + lines.size() + " line(s)");
    }

    /**
     * Feature_Envy_Method on {@link Order}. Reads five fields of {@code Order}
     * directly ({@code id}, {@code shippingAddressOverride}, {@code customer},
     * {@code items}, {@code placedAt}); declaring-class accesses on
     * {@code OrderProcessor} are limited to two ({@code shippingCalculator},
     * {@code auditTrail}). Five foreign Order accesses strictly exceed two
     * self-class accesses.
     */
    public double dispatchShipmentFor(Order o) {
        String orderId = o.id;
        String override = o.shippingAddressOverride;
        Customer recipient = o.customer;
        List<OrderItem> lines = o.items;
        Object placed = o.placedAt;
        int unitCount = 0;
        if (lines != null) {
            for (int i = 0; i < lines.size(); i++) {
                unitCount = unitCount + lines.get(i).quantity;
            }
        }
        double shipping = this.shippingCalculator.calculateShippingForOrder(o);
        String dest;
        if (override != null && !override.isEmpty()) {
            dest = override;
        } else if (recipient != null) {
            dest = recipient.city;
        } else {
            dest = "<unknown>";
        }
        this.auditTrail.add("dispatchShipmentFor: " + orderId + " units=" + unitCount
                + " dest=" + dest + " placed=" + placed + " cost=" + shipping);
        return shipping;
    }

    /** Builds an {@link Invoice} for {@code o} and stores it as current. */
    public Invoice generateInvoiceFor(Order o) {
        Invoice inv = new Invoice();
        inv.invoiceNumber = "INV-" + o.id;
        inv.order = o;
        inv.issuedAt = o.placedAt;
        inv.lineItems = o.items;
        inv.notes = "auto-generated";
        this.currentInvoice = inv;
        double grand = inv.computeInvoiceGrandTotalWithTax();
        this.auditTrail.add("generateInvoiceFor: " + inv.invoiceNumber + " grand=" + grand);
        return inv;
    }

    /** Applies a loyalty-tier-derived discount by mutating {@link Customer}. */
    public double applyLoyaltyDiscountFor(Order o) {
        Customer c = o.customer;
        String tier = c.evaluateLoyaltyTier();
        double discount;
        if (tier != null && tier.contains("PLATINUM")) {
            discount = 0.20d;
        } else if (tier != null && tier.contains("GOLD")) {
            discount = 0.10d;
        } else if (tier != null && tier.contains("SILVER")) {
            discount = 0.05d;
        } else {
            discount = 0.0d;
        }
        double base = o.computeOrderTotalWithTax();
        double saved = base * discount;
        c.recordLoyaltyEarning(base - saved);
        this.auditTrail.add("applyLoyaltyDiscountFor: " + o.id + " tier=" + tier + " saved=" + saved);
        return saved;
    }

    /**
     * Shotgun_Surgery_Concern (tax). Inlines the literal {@code 0.0825}
     * directly in the body. No {@code static final}, no shared helper.
     */
    public double recomputeOrderTotalWithTax(Order o) {
        List<OrderItem> lines = o.items;
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
        double total = subtotal + tax;
        this.auditTrail.add("recomputeOrderTotalWithTax: " + o.id + " total=" + total);
        return total;
    }

    /**
     * Shotgun_Surgery_Concern (currency). Inlines the literal {@code 0.92}
     * directly in the body. No {@code static final}, no shared helper.
     */
    public double convertOrderToCurrency(Order o) {
        List<OrderItem> lines = o.items;
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
        double euros = totalUsd * 0.92d;
        if (euros < 0.0d) {
            euros = 0.0d;
        }
        this.auditTrail.add("convertOrderToCurrency: " + o.id + " eur=" + euros);
        return euros;
    }

    /**
     * Feature_Envy_Method on {@link Order}. Reads five fields of {@code Order}
     * ({@code items}, {@code customer}, {@code id}, {@code paid},
     * {@code placedAt}); declaring-class accesses on {@code OrderProcessor}
     * are limited to one ({@code auditTrail}). Five foreign Order accesses
     * strictly exceed one self-class access.
     */
    public boolean flagSuspiciousOrder(Order o) {
        List<OrderItem> lines = o.items;
        Customer cust = o.customer;
        String orderId = o.id;
        boolean paidFlag = o.paid;
        Object placed = o.placedAt;
        int totalQty = 0;
        if (lines != null) {
            for (int i = 0; i < lines.size(); i++) {
                totalQty = totalQty + lines.get(i).quantity;
            }
        }
        boolean suspicious;
        if (totalQty > 500) {
            suspicious = true;
        } else if (!paidFlag && totalQty > 100) {
            suspicious = true;
        } else if (cust == null) {
            suspicious = true;
        } else if (placed == null) {
            suspicious = true;
        } else {
            suspicious = false;
        }
        this.auditTrail.add("flagSuspiciousOrder: " + orderId + " qty=" + totalQty + " flagged=" + suspicious);
        return suspicious;
    }
}
