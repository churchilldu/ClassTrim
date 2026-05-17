package org.classtrim.smellydemo.verify;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Property 5: Smelly_Method structural eligibility.
 *
 * <p>For every method registered as a Smelly_Method in the design's
 * smelly-method registry, asserts the method is {@code public}, non-{@code
 * static}, not a constructor ({@code <init>}), lacks the {@code @Override}
 * annotation, and is not a pure field-return getter or pure
 * parameter-to-field setter.
 *
 * <p>The registry is materialized inline below and was verified against the
 * actual {@code smelly-demo} sources at {@code
 * smelly-demo/src/main/java/org/classtrim/demo/ecommerce/*.java} — every
 * {@code (className, methodName)} entry corresponds to an actually-declared
 * public, non-static, non-{@code @Override} instance method whose body is
 * neither a pure getter nor a pure setter.
 *
 * <p>Each registry entry is checked through {@link MethodClassifier}'s
 * individual predicates ({@code isPublic}, {@code isStatic}, {@code
 * isConstructor}, {@code hasOverrideAnnotation}, {@code isPureGetter},
 * {@code isPureSetter}) so a failure pinpoints the offending rule, and then
 * the conjoined {@link MethodClassifier#isSmellyMethodEligible} predicate is
 * asserted as a defensive companion check.
 *
 * <p><b>Validates: Requirements 3.5, 3.6, 3.7, 3.8, 3.9</b>
 */
public class Property5SmellyMethodEligibilityTest {

    /**
     * Build the smelly-method registry from the design and verify it inline.
     * Keys are Demo_Class simple names; values are sets of declared method
     * names that are Smelly_Methods on that class.
     */
    private static Map<String, Set<String>> smellyMethodRegistry() {
        Map<String, Set<String>> registry = new LinkedHashMap<>();
        registry.put("Customer", new LinkedHashSet<>(Arrays.asList(
                "evaluateLoyaltyTier",
                "summarizeOrderForCustomer",
                "recordLoyaltyEarning",
                "renderMailingAddress")));
        registry.put("Product", new LinkedHashSet<>(Collections.singletonList(
                "priceInEur")));
        registry.put("OrderItem", new LinkedHashSet<>(Arrays.asList(
                "computeLineTaxedTotal",
                "convertLineTotalToEur",
                "summarizeForLogistics")));
        registry.put("Order", new LinkedHashSet<>(Arrays.asList(
                "formatShippingLabel",
                "computeCustomerLifetimeValue",
                "computeOrderTotalWithTax",
                "convertOrderTotalToEur",
                "markPaid")));
        registry.put("Inventory", new LinkedHashSet<>(Arrays.asList(
                "reserveStock",
                "releaseStock",
                "availableStock")));
        registry.put("ShippingCalculator", new LinkedHashSet<>(Collections.singletonList(
                "calculateShippingForOrder")));
        registry.put("Invoice", new LinkedHashSet<>(Arrays.asList(
                "renderInvoiceLines",
                "computeInvoiceGrandTotalWithTax",
                "convertGrandTotalToEur",
                "auditOrderItems")));
        registry.put("OrderProcessor", new LinkedHashSet<>(Arrays.asList(
                "processNewOrder",
                "chargeCustomer",
                "reserveInventoryFor",
                "releaseInventoryFor",
                "dispatchShipmentFor",
                "generateInvoiceFor",
                "applyLoyaltyDiscountFor",
                "recomputeOrderTotalWithTax",
                "convertOrderToCurrency",
                "flagSuspiciousOrder")));
        return Collections.unmodifiableMap(registry);
    }

    @Test
    public void everyRegisteredSmellyMethodSatisfiesAllFiveStructuralPredicates() {
        Map<String, DemoClassModel> byName = SmellyDemoLoader.loadAllDemoClassModels();
        Map<String, Set<String>> registry = smellyMethodRegistry();

        // Every registered class name must be present in the loaded inventory.
        for (String className : registry.keySet()) {
            assertNotNull(
                    "smelly-method registry references unknown Demo_Class `" + className + "`",
                    byName.get(className));
        }

        for (Map.Entry<String, Set<String>> classEntry : registry.entrySet()) {
            String className = classEntry.getKey();
            DemoClassModel owner = byName.get(className);

            for (String methodName : classEntry.getValue()) {
                List<DemoMethodModel> matches = methodsNamed(owner, methodName);
                String label = className + "." + methodName;

                assertFalse(
                        "Smelly_Method `" + label + "` is registered but not declared on `"
                                + className + "`",
                        matches.isEmpty());

                for (DemoMethodModel m : matches) {
                    String overloadLabel = label + m.descriptor;

                    // Requirement 3.5: public
                    assertTrue(
                            "Smelly_Method `" + overloadLabel + "` must be declared public",
                            MethodClassifier.isPublic(m));

                    // Requirement 3.6: non-static
                    assertFalse(
                            "Smelly_Method `" + overloadLabel + "` must not be static",
                            MethodClassifier.isStatic(m));

                    // Requirement 3.7: not a constructor
                    assertFalse(
                            "Smelly_Method `" + overloadLabel + "` must not be a constructor (<init>)",
                            MethodClassifier.isConstructor(m));

                    // Requirement 3.8: lacks @Override
                    assertFalse(
                            "Smelly_Method `" + overloadLabel + "` must not be annotated with @Override",
                            MethodClassifier.hasOverrideAnnotation(m));

                    // Requirement 3.9: not a pure getter or pure setter body
                    assertFalse(
                            "Smelly_Method `" + overloadLabel + "` must not be a pure field-return getter",
                            MethodClassifier.isPureGetter(owner, m));
                    assertFalse(
                            "Smelly_Method `" + overloadLabel + "` must not be a pure"
                                    + " parameter-to-field setter",
                            MethodClassifier.isPureSetter(owner, m));

                    // Defensive companion: the conjoined predicate must agree.
                    assertTrue(
                            "Smelly_Method `" + overloadLabel + "` must satisfy the conjoined"
                                    + " MethodClassifier.isSmellyMethodEligible predicate",
                            MethodClassifier.isSmellyMethodEligible(owner, m));
                }
            }
        }
    }

    /** Collect every method on {@code owner} whose name equals {@code methodName}. */
    private static List<DemoMethodModel> methodsNamed(DemoClassModel owner, String methodName) {
        List<DemoMethodModel> out = new ArrayList<>();
        for (DemoMethodModel m : owner.methods) {
            if (methodName.equals(m.name)) {
                out.add(m);
            }
        }
        if (out.isEmpty()) {
            // Build a hint listing what *is* declared so registry drift is easy to diagnose.
            StringBuilder declared = new StringBuilder();
            for (DemoMethodModel m : owner.methods) {
                if (declared.length() > 0) {
                    declared.append(", ");
                }
                declared.append(m.name).append(m.descriptor);
            }
            fail("No method named `" + methodName + "` declared on `"
                    + owner.simpleName() + "`. Declared methods: [" + declared + "]");
        }
        return out;
    }
}
