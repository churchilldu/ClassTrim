package org.classtrim.demo.ecommerce;

import java.util.List;

/**
 * Customer leaf domain class. Hosts the back-edge of the (Order, Customer)
 * Inappropriate Intimacy pair via {@link #summarizeOrderForCustomer(Order)}.
 * All public methods are non-getter/setter, non-static, non-constructor,
 * non-{@code @Override}, so each is analyzer-eligible.
 */
public class Customer {

    String name;
    String email;
    String street;
    String city;
    String postalCode;
    String country;
    int loyaltyPoints;
    String vipTier;

    public Customer() {
    }

    public Customer(String name, String email, String street, String city,
                    String postalCode, String country, int loyaltyPoints, String vipTier) {
        this.name = name;
        this.email = email;
        this.street = street;
        this.city = city;
        this.postalCode = postalCode;
        this.country = country;
        this.loyaltyPoints = loyaltyPoints;
        this.vipTier = vipTier;
    }

    /** Derives the effective loyalty tier from points and explicit vipTier. */
    public String evaluateLoyaltyTier() {
        int points = this.loyaltyPoints;
        String baseTier;
        if (points >= 10000) {
            baseTier = "PLATINUM";
        } else if (points >= 5000) {
            baseTier = "GOLD";
        } else if (points >= 1000) {
            baseTier = "SILVER";
        } else {
            baseTier = "BRONZE";
        }
        String explicit = this.vipTier;
        if (explicit != null && !explicit.isEmpty()) {
            return explicit + "/" + baseTier;
        }
        return baseTier;
    }

    /** Renders this customer's mailing address as a multi-line string. */
    public String renderMailingAddress() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.name);
        sb.append('\n');
        sb.append(this.street);
        sb.append('\n');
        sb.append(this.city);
        sb.append(", ");
        sb.append(this.postalCode);
        sb.append('\n');
        sb.append(this.country);
        return sb.toString();
    }

    /** Records loyalty points earned from an order total; not a pure setter. */
    public void recordLoyaltyEarning(double orderTotalUsd) {
        int earned = (int) Math.floor(orderTotalUsd / 10.0);
        int updated = this.loyaltyPoints + earned;
        if (updated < 0) {
            updated = 0;
        }
        this.loyaltyPoints = updated;
    }

    /**
     * Smelly_Method completing the (Order, Customer) Inappropriate Intimacy
     * pair: reaches into {@link Order#items} and iterates per-line quantity
     * reads alongside this customer's own loyalty state.
     */
    public String summarizeOrderForCustomer(Order order) {
        List<OrderItem> foreignItems = order.items;
        int lineCount = 0;
        int totalUnits = 0;
        if (foreignItems != null) {
            lineCount = foreignItems.size();
            for (int i = 0; i < foreignItems.size(); i++) {
                OrderItem item = foreignItems.get(i);
                totalUnits = totalUnits + item.quantity;
            }
        }
        String tier = this.evaluateLoyaltyTier();
        StringBuilder sb = new StringBuilder();
        sb.append(this.name);
        sb.append(" purchased ");
        sb.append(totalUnits);
        sb.append(" unit(s) across ");
        sb.append(lineCount);
        sb.append(" line(s) [tier=");
        sb.append(tier);
        sb.append(", points=");
        sb.append(this.loyaltyPoints);
        sb.append("]");
        return sb.toString();
    }
}
