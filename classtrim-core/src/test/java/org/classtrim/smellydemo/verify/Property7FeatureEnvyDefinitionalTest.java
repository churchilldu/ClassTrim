package org.classtrim.smellydemo.verify;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Property 7: Feature Envy definitional invariant.
 *
 * <p>For every method designated as a Feature_Envy_Method in the design's
 * smelly-method registry, this test asserts the bytecode performs strictly
 * more field reads plus method invocations on one specific foreign Demo_Class
 * than on the declaring class. The metric is computed by
 * {@link BytecodeAccessCounter#fieldReadsPlusInvokesByOwner(DemoMethodModel)},
 * keyed by owner internal name.
 *
 * <p>Concrete registry (declaring class, method name, envied class), drawn
 * directly from the design's Feature_Envy_Method assignments and confirmed
 * against the smelly-demo source:
 *
 * <ul>
 *   <li>{@code Order.formatShippingLabel} envies {@code Customer}</li>
 *   <li>{@code Order.computeCustomerLifetimeValue} envies {@code Customer}</li>
 *   <li>{@code Invoice.renderInvoiceLines} envies {@code Order}</li>
 *   <li>{@code ShippingCalculator.calculateShippingForOrder} envies {@code Order}</li>
 *   <li>{@code OrderItem.summarizeForLogistics} envies {@code Product}</li>
 *   <li>{@code OrderProcessor.chargeCustomer} envies {@code Customer}</li>
 *   <li>{@code OrderProcessor.dispatchShipmentFor} envies {@code Order}</li>
 *   <li>{@code OrderProcessor.flagSuspiciousOrder} envies {@code Order}</li>
 * </ul>
 *
 * <p>Overload handling: when multiple methods on the declaring class share
 * the registry's name, the test asserts that <em>at least one</em> overload
 * satisfies the property. This keeps the registry a method-name claim rather
 * than a descriptor-level claim while still pinpointing the offending
 * declaration on failure.
 *
 * <p><b>Validates: Requirements 4.2, 4.6</b>
 */
public class Property7FeatureEnvyDefinitionalTest {

    /** Immutable triple naming a single Feature_Envy_Method registry entry. */
    private static final class FemEntry {
        final String declaringClass;
        final String methodName;
        final String enviedClass;

        FemEntry(String declaringClass, String methodName, String enviedClass) {
            this.declaringClass = declaringClass;
            this.methodName = methodName;
            this.enviedClass = enviedClass;
        }

        @Override
        public String toString() {
            return declaringClass + "." + methodName + " envies " + enviedClass;
        }
    }

    /**
     * Build the Feature_Envy_Method registry as an unmodifiable list of
     * {@code (declaringClass, methodName, enviedClass)} triples.
     */
    private static List<FemEntry> femRegistry() {
        List<FemEntry> entries = new ArrayList<>(Arrays.asList(
                new FemEntry("Order", "formatShippingLabel", "Customer"),
                new FemEntry("Order", "computeCustomerLifetimeValue", "Customer"),
                new FemEntry("Invoice", "renderInvoiceLines", "Order"),
                new FemEntry("ShippingCalculator", "calculateShippingForOrder", "Order"),
                new FemEntry("OrderItem", "summarizeForLogistics", "Product"),
                new FemEntry("OrderProcessor", "chargeCustomer", "Customer"),
                new FemEntry("OrderProcessor", "dispatchShipmentFor", "Order"),
                new FemEntry("OrderProcessor", "flagSuspiciousOrder", "Order")));
        return Collections.unmodifiableList(entries);
    }

    @Test
    public void everyFeatureEnvyMethodAccessesEnviedClassMoreThanSelf() {
        Map<String, DemoClassModel> byName = SmellyDemoLoader.loadAllDemoClassModels();

        List<String> failures = new ArrayList<>();

        for (FemEntry entry : femRegistry()) {
            DemoClassModel declaring = byName.get(entry.declaringClass);
            assertNotNull(
                    "Declaring Demo_Class not found in inventory: " + entry.declaringClass,
                    declaring);

            // Locate every overload on the declaring class with the registry's
            // method name. Overload-aware: assert at least one overload satisfies
            // the Feature Envy property.
            List<DemoMethodModel> overloads = new ArrayList<>();
            for (DemoMethodModel m : declaring.methods) {
                if (entry.methodName.equals(m.name)) {
                    overloads.add(m);
                }
            }
            if (overloads.isEmpty()) {
                failures.add("No method named `" + entry.methodName
                        + "` found on declaring class `" + entry.declaringClass
                        + "` (FEM registry entry: " + entry + ")");
                continue;
            }

            String declaringInternal =
                    SmellyDemoLoader.simpleNameToInternalName(entry.declaringClass);
            String enviedInternal =
                    SmellyDemoLoader.simpleNameToInternalName(entry.enviedClass);

            // Track the best overload (max foreign-vs-self margin) so the failure
            // message reports the most useful counts when none qualify.
            DemoMethodModel bestOverload = overloads.get(0);
            int bestForeign = 0;
            int bestSelf = 0;
            boolean anyOverloadSatisfies = false;

            for (DemoMethodModel m : overloads) {
                Map<String, Integer> count =
                        BytecodeAccessCounter.fieldReadsPlusInvokesByOwner(m);
                int foreign = count.getOrDefault(enviedInternal, 0);
                int self = count.getOrDefault(declaringInternal, 0);
                if (foreign > self) {
                    anyOverloadSatisfies = true;
                    bestOverload = m;
                    bestForeign = foreign;
                    bestSelf = self;
                    break;
                }
                // Track the overload with the largest (foreign - self) margin so
                // the failure diagnostic surfaces the closest near-miss.
                if ((foreign - self) > (bestForeign - bestSelf)) {
                    bestOverload = m;
                    bestForeign = foreign;
                    bestSelf = self;
                }
            }

            if (!anyOverloadSatisfies) {
                Map<String, Integer> diagCount =
                        BytecodeAccessCounter.fieldReadsPlusInvokesByOwner(bestOverload);
                failures.add(
                        "Feature_Envy_Method `" + entry.declaringClass + "."
                                + entry.methodName + "` (descriptor "
                                + bestOverload.descriptor + ") must perform strictly more"
                                + " field reads + method invocations on envied class `"
                                + entry.enviedClass + "` than on declaring class `"
                                + entry.declaringClass + "`. Observed: foreign="
                                + bestForeign + " self=" + bestSelf
                                + " (registry entry: " + entry
                                + ", full counts by owner: " + diagCount + ")");
            }
        }

        if (!failures.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Feature_Envy_Method definitional invariant violated for ");
            sb.append(failures.size());
            sb.append(" registry entr");
            sb.append(failures.size() == 1 ? "y" : "ies");
            sb.append(":\n");
            for (String f : failures) {
                sb.append("  - ");
                sb.append(f);
                sb.append('\n');
            }
            fail(sb.toString());
        }
    }

    /**
     * Companion check: confirms the Feature_Envy_Method registry is itself
     * non-empty and that every declared declaring class is part of the
     * canonical Demo_Class inventory. Guards against typos in the registry
     * causing the main check to silently skip entries.
     */
    @Test
    public void femRegistryIsWellFormedAgainstDemoClassInventory() {
        List<FemEntry> registry = femRegistry();
        assertTrue(
                "FEM registry must not be empty",
                !registry.isEmpty());

        for (FemEntry entry : registry) {
            assertTrue(
                    "FEM registry references unknown declaring class: "
                            + entry.declaringClass,
                    SmellyDemoLoader.EXPECTED_DEMO_CLASS_NAMES.contains(entry.declaringClass));
            assertTrue(
                    "FEM registry references unknown envied class: "
                            + entry.enviedClass,
                    SmellyDemoLoader.EXPECTED_DEMO_CLASS_NAMES.contains(entry.enviedClass));
            assertTrue(
                    "FEM registry entry must envy a foreign class (declaring != envied): "
                            + entry,
                    !entry.declaringClass.equals(entry.enviedClass));
        }
    }
}
