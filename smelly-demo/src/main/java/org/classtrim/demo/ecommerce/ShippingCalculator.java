package org.classtrim.demo.ecommerce;

import java.util.List;

/**
 * Service-style class that prices shipping for an {@link Order}. Hosts the
 * Feature_Envy_Method required by the design at section 4.5:
 * {@link #calculateShippingForOrder(Order)} performs strictly more direct
 * field reads on {@link Order} than on its own declaring class.
 *
 * <p>Self {@code ShippingCalculator} GETFIELDs: three (baseRateUsd,
 * perKgRateUsd, expeditedSurchargeUsd, each cached once). Foreign
 * {@code Order} GETFIELDs: five (items, shippingAddressOverride, customer,
 * currency, id). Five strictly exceeds three.</p>
 */
public class ShippingCalculator {

    double baseRateUsd;
    double perKgRateUsd;
    double expeditedSurchargeUsd;

    public ShippingCalculator() {
    }

    public ShippingCalculator(double baseRateUsd, double perKgRateUsd, double expeditedSurchargeUsd) {
        this.baseRateUsd = baseRateUsd;
        this.perKgRateUsd = perKgRateUsd;
        this.expeditedSurchargeUsd = expeditedSurchargeUsd;
    }

    /** Feature_Envy_Method on {@link Order}. */
    public double calculateShippingForOrder(Order o) {
        double base = this.baseRateUsd;
        double perKg = this.perKgRateUsd;
        double surcharge = this.expeditedSurchargeUsd;

        List<OrderItem> lines = o.items;
        String override = o.shippingAddressOverride;
        Customer recipient = o.customer;
        String orderCurrency = o.currency;
        String orderId = o.id;

        double totalWeightKg = 0.0d;
        int totalUnits = 0;
        if (lines != null) {
            for (int i = 0; i < lines.size(); i++) {
                OrderItem line = lines.get(i);
                int qty = line.quantity;
                Product p = line.product;
                double unitWeight;
                if (p != null) {
                    unitWeight = p.weightKg;
                } else {
                    unitWeight = 0.0d;
                }
                totalWeightKg = totalWeightKg + (unitWeight * ((double) qty));
                totalUnits = totalUnits + qty;
            }
        }

        double cost = base + (perKg * totalWeightKg);

        if (override != null && !override.isEmpty()) {
            cost = cost + surcharge;
        }

        String destinationCountry;
        if (recipient != null) {
            destinationCountry = recipient.country;
        } else {
            destinationCountry = null;
        }
        if (destinationCountry != null && !"US".equals(destinationCountry)) {
            cost = cost + (surcharge * 2.0d);
        }

        if (totalUnits > 10) {
            cost = cost + (((double) totalUnits) * 0.05d);
        }

        if ("USD".equals(orderCurrency)) {
            if (cost < base) {
                cost = base;
            }
        } else {
            if (cost <= 0.0d) {
                cost = base;
            }
        }

        if (orderId != null && orderId.startsWith("VIP-")) {
            cost = cost - 1.0d;
            if (cost < 0.0d) {
                cost = 0.0d;
            }
        }

        return cost;
    }
}
