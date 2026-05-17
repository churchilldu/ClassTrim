package org.classtrim.smellydemo.verify;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Property 12: Shotgun Surgery inlined-literal invariant.
 *
 * <p>For every method registered in the tax-computation registry, this test
 * asserts the method's bytecode contains an {@code LDC} push of the literal
 * {@code 0.0825} and does not delegate to a single shared helper class.
 * Symmetrically, every method in the currency-conversion registry must contain
 * an {@code LDC} push of the literal {@code 0.92} and must not delegate to a
 * shared helper.
 *
 * <p>Tax-computation registry (methods that inline {@code 0.0825}):
 * <ul>
 *     <li>{@code OrderItem.computeLineTaxedTotal}</li>
 *     <li>{@code Order.computeOrderTotalWithTax}</li>
 *     <li>{@code Invoice.computeInvoiceGrandTotalWithTax}</li>
 *     <li>{@code OrderProcessor.recomputeOrderTotalWithTax}</li>
 * </ul>
 *
 * <p>Currency-conversion registry (methods that inline {@code 0.92}):
 * <ul>
 *     <li>{@code Product.priceInEur}</li>
 *     <li>{@code OrderItem.convertLineTotalToEur}</li>
 *     <li>{@code Order.convertOrderTotalToEur}</li>
 *     <li>{@code Invoice.convertGrandTotalToEur}</li>
 *     <li>{@code OrderProcessor.convertOrderToCurrency}</li>
 * </ul>
 *
 * <p>The "no shared helper" rule is enforced operationally: each method's
 * inlined arithmetic is proved by {@link BytecodeAccessCounter#containsLdcDouble},
 * and additionally we scan the method's invocation owners and assert no owner
 * has a simple name in the prohibited shared-helper name set
 * ({@code TaxCalculator}, {@code TaxHelper}, {@code CurrencyConverter},
 * {@code CurrencyHelper}, {@code MoneyUtil}, {@code PricingHelper},
 * {@code RatesHelper}, {@code MoneyHelper}, {@code Money}, {@code MathUtil}).
 * Since none of these classes exist in the Demo_Class inventory the check is
 * trivially satisfied today, but it is encoded explicitly so the property
 * documents the prohibition against introducing such a helper later.
 *
 * <p>Overload handling: when multiple methods on the declaring class share the
 * registry's name, the test asserts that <em>at least one</em> overload pushes
 * the required literal. In practice these names are unique on their declaring
 * class, but the check is overload-aware to match the style of Property 7.
 *
 * <p><b>Validates: Requirements 7.3, 7.4</b>
 */
public class Property12ShotgunSurgeryInlinedLiteralTest {

    /** Numeric literal expected inline in every tax-computation method. */
    private static final double TAX_LITERAL = 0.0825d;

    /** Numeric literal expected inline in every currency-conversion method. */
    private static final double CURRENCY_LITERAL = 0.92d;

    /**
     * Simple names of prohibited shared-helper classes. Encodes the design's
     * "no shared helper" rule so this property documents the prohibition even
     * though none of these classes exist in the inventory today.
     */
    private static final Set<String> FORBIDDEN_HELPER_SIMPLE_NAMES =
            Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
                    "TaxCalculator",
                    "TaxHelper",
                    "CurrencyConverter",
                    "CurrencyHelper",
                    "MoneyUtil",
                    "PricingHelper",
                    "RatesHelper",
                    "MoneyHelper",
                    "Money",
                    "MathUtil")));

    /** Immutable triple naming a single Shotgun_Surgery registry entry. */
    private static final class ShotgunEntry {
        final String declaringClass;
        final String methodName;
        final double literal;

        ShotgunEntry(String declaringClass, String methodName, double literal) {
            this.declaringClass = declaringClass;
            this.methodName = methodName;
            this.literal = literal;
        }

        @Override
        public String toString() {
            return declaringClass + "." + methodName + " inlines " + literal;
        }
    }

    private static List<ShotgunEntry> taxRegistry() {
        return Collections.unmodifiableList(new ArrayList<>(Arrays.asList(
                new ShotgunEntry("OrderItem", "computeLineTaxedTotal", TAX_LITERAL),
                new ShotgunEntry("Order", "computeOrderTotalWithTax", TAX_LITERAL),
                new ShotgunEntry("Invoice", "computeInvoiceGrandTotalWithTax", TAX_LITERAL),
                new ShotgunEntry("OrderProcessor", "recomputeOrderTotalWithTax", TAX_LITERAL))));
    }

    private static List<ShotgunEntry> currencyRegistry() {
        return Collections.unmodifiableList(new ArrayList<>(Arrays.asList(
                new ShotgunEntry("Product", "priceInEur", CURRENCY_LITERAL),
                new ShotgunEntry("OrderItem", "convertLineTotalToEur", CURRENCY_LITERAL),
                new ShotgunEntry("Order", "convertOrderTotalToEur", CURRENCY_LITERAL),
                new ShotgunEntry("Invoice", "convertGrandTotalToEur", CURRENCY_LITERAL),
                new ShotgunEntry("OrderProcessor", "convertOrderToCurrency", CURRENCY_LITERAL))));
    }

    @Test
    public void taxRegistryMethodsInlineLiteralAndAvoidSharedHelpers() {
        assertRegistryInlinesLiteralAndAvoidsSharedHelpers(
                "tax-computation", taxRegistry());
    }

    @Test
    public void currencyRegistryMethodsInlineLiteralAndAvoidSharedHelpers() {
        assertRegistryInlinesLiteralAndAvoidsSharedHelpers(
                "currency-conversion", currencyRegistry());
    }

    /**
     * Companion guard: the registries must reference only Demo_Classes from
     * the canonical inventory and must not be empty, so a typo cannot cause
     * the main checks to silently skip entries.
     */
    @Test
    public void registriesAreWellFormedAgainstDemoClassInventory() {
        List<ShotgunEntry> tax = taxRegistry();
        List<ShotgunEntry> currency = currencyRegistry();

        assertTrue("Tax registry must not be empty", !tax.isEmpty());
        assertTrue("Currency registry must not be empty", !currency.isEmpty());

        for (ShotgunEntry e : tax) {
            assertTrue(
                    "Tax registry references unknown declaring class: " + e.declaringClass,
                    SmellyDemoLoader.EXPECTED_DEMO_CLASS_NAMES.contains(e.declaringClass));
            assertTrue(
                    "Tax registry literal must be 0.0825 for " + e,
                    Double.compare(e.literal, TAX_LITERAL) == 0);
        }
        for (ShotgunEntry e : currency) {
            assertTrue(
                    "Currency registry references unknown declaring class: " + e.declaringClass,
                    SmellyDemoLoader.EXPECTED_DEMO_CLASS_NAMES.contains(e.declaringClass));
            assertTrue(
                    "Currency registry literal must be 0.92 for " + e,
                    Double.compare(e.literal, CURRENCY_LITERAL) == 0);
        }
    }

    // ---------------------------------------------------------------
    // shared check
    // ---------------------------------------------------------------

    private static void assertRegistryInlinesLiteralAndAvoidsSharedHelpers(
            String registryLabel, List<ShotgunEntry> registry) {
        Map<String, DemoClassModel> byName = SmellyDemoLoader.loadAllDemoClassModels();

        List<String> failures = new ArrayList<>();

        for (ShotgunEntry entry : registry) {
            DemoClassModel declaring = byName.get(entry.declaringClass);
            assertNotNull(
                    "Declaring Demo_Class not found in inventory: " + entry.declaringClass,
                    declaring);

            // Locate every overload sharing the registry's method name.
            List<DemoMethodModel> overloads = new ArrayList<>();
            for (DemoMethodModel m : declaring.methods) {
                if (entry.methodName.equals(m.name)) {
                    overloads.add(m);
                }
            }
            if (overloads.isEmpty()) {
                failures.add("No method named `" + entry.methodName
                        + "` found on declaring class `" + entry.declaringClass
                        + "` (registry entry: " + entry + ")");
                continue;
            }

            // (1) at-least-one-overload pushes the required literal via LDC.
            DemoMethodModel matchingOverload = null;
            for (DemoMethodModel m : overloads) {
                if (BytecodeAccessCounter.containsLdcDouble(m, entry.literal)) {
                    matchingOverload = m;
                    break;
                }
            }
            if (matchingOverload == null) {
                StringBuilder ldcSummary = new StringBuilder();
                for (DemoMethodModel m : overloads) {
                    ldcSummary.append("\n      ").append(m.name).append(m.descriptor)
                            .append(" LDC constants=")
                            .append(BytecodeAccessCounter.ldcConstants(m));
                }
                failures.add("Shotgun_Surgery method `" + entry.declaringClass + "."
                        + entry.methodName + "` (" + registryLabel
                        + ") must contain an LDC push of the literal "
                        + entry.literal
                        + " inlined directly in its body. Observed overload(s):"
                        + ldcSummary);
                continue;
            }

            // (2) "no shared helper" defensive check on the matching overload.
            Map<String, Integer> invokes = BytecodeAccessCounter.invokesByOwner(matchingOverload);
            List<String> offendingOwners = new ArrayList<>();
            for (String ownerInternal : invokes.keySet()) {
                String simple = SmellyDemoLoader.simpleNameFromInternalName(ownerInternal);
                if (FORBIDDEN_HELPER_SIMPLE_NAMES.contains(simple)) {
                    offendingOwners.add(ownerInternal);
                }
            }
            if (!offendingOwners.isEmpty()) {
                Collections.sort(offendingOwners);
                failures.add("Shotgun_Surgery method `" + entry.declaringClass + "."
                        + entry.methodName + "` (" + registryLabel
                        + ") must not delegate to a shared helper class, but"
                        + " its bytecode invokes methods on owner(s) "
                        + offendingOwners
                        + " whose simple name appears in the forbidden helper"
                        + " set " + new TreeSet<>(FORBIDDEN_HELPER_SIMPLE_NAMES)
                        + ". Full invocation owners: " + invokes
                        + " (registry entry: " + entry + ")");
            }
        }

        if (!failures.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Property 12 (").append(registryLabel)
                    .append(") inlined-literal invariant violated for ")
                    .append(failures.size())
                    .append(" registry entr")
                    .append(failures.size() == 1 ? "y" : "ies")
                    .append(":\n");
            for (String f : failures) {
                sb.append("  - ").append(f).append('\n');
            }
            fail(sb.toString());
        }
    }
}
