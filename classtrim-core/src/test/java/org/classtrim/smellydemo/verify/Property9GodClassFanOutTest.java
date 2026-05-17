package org.classtrim.smellydemo.verify;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Property 9: God Class fan-out and method count on {@code OrderProcessor}.
 *
 * <p>For the produced {@code OrderProcessor} class, the count of public,
 * non-static, non-constructor, non-{@code @Override}, non-pure-getter,
 * non-pure-setter instance methods is at least {@code 8}, and the set of
 * distinct foreign Demo_Classes whose fields it reads or whose methods it
 * invokes (across all its methods' bytecode) has cardinality at least
 * {@code 5}.
 *
 * <p>The "smelly-method-eligible" predicate is sourced from
 * {@link MethodClassifier#isSmellyMethodEligible(DemoClassModel, DemoMethodModel)}.
 * The fan-out aggregation per method uses
 * {@link BytecodeAccessCounter#fieldReadsPlusInvokesByOwner(DemoMethodModel)}
 * — i.e. {@code GETFIELD} owners plus {@code INVOKE*} owners (excluding
 * {@code INVOKEDYNAMIC}). Owners are filtered down to the canonical
 * Demo_Class inventory via
 * {@link SmellyDemoLoader#isDemoClassInternalName(String)}, and the
 * declaring class itself is excluded so only <em>foreign</em> targets count.
 *
 * <p><b>Validates: Requirements 5.1, 5.2</b>
 */
public class Property9GodClassFanOutTest {

    private static final int MIN_SMELLY_METHOD_COUNT = 8;
    private static final int MIN_FOREIGN_DEMO_CLASS_FAN_OUT = 5;

    @Test
    public void orderProcessorHasGodClassMethodCountAndFanOut() {
        Map<String, DemoClassModel> byName = SmellyDemoLoader.loadAllDemoClassModels();
        DemoClassModel orderProcessor = byName.get("OrderProcessor");
        assertNotNull(
                "Demo_Class inventory must contain OrderProcessor; loaded keys = "
                        + new TreeSet<>(byName.keySet())
                        + ". Run `mvn -pl smelly-demo -am compile` first.",
                orderProcessor);

        // 1. Count smelly-method-eligible methods on OrderProcessor.
        List<DemoMethodModel> smellyEligible = orderProcessor.methods.stream()
                .filter(m -> MethodClassifier.isSmellyMethodEligible(orderProcessor, m))
                .sorted(Comparator.comparing(m -> m.name + m.descriptor))
                .collect(Collectors.toList());

        // 2. Aggregate foreign Demo_Class fan-out across every method on
        //    OrderProcessor (constructors and overrides included; their
        //    bytecode still contributes to coupling fan-out).
        Set<String> foreignDemoClassInternalNames = new LinkedHashSet<>();
        for (DemoMethodModel m : orderProcessor.methods) {
            Map<String, Integer> byOwner = BytecodeAccessCounter.fieldReadsPlusInvokesByOwner(m);
            for (String owner : byOwner.keySet()) {
                if (!SmellyDemoLoader.isDemoClassInternalName(owner)) {
                    continue;
                }
                if (orderProcessor.internalName.equals(owner)) {
                    continue;
                }
                foreignDemoClassInternalNames.add(owner);
            }
        }

        // Sorted, simple-named view for stable failure output.
        Set<String> foreignDemoClassSimpleNames = new TreeSet<>();
        for (String internalName : foreignDemoClassInternalNames) {
            foreignDemoClassSimpleNames.add(
                    SmellyDemoLoader.simpleNameFromInternalName(internalName));
        }

        List<String> smellyEligibleSignatures = new ArrayList<>(smellyEligible.size());
        for (DemoMethodModel m : smellyEligible) {
            smellyEligibleSignatures.add(m.name + m.descriptor);
        }

        // 3. Assert smelly-method-eligible method count.
        assertTrue(
                "OrderProcessor must declare at least " + MIN_SMELLY_METHOD_COUNT
                        + " public, non-static, non-constructor, non-@Override,"
                        + " non-pure-getter, non-pure-setter instance methods"
                        + " (Property 9 / Requirement 5.1)."
                        + " Found " + smellyEligibleSignatures.size() + ": "
                        + smellyEligibleSignatures
                        + ". Foreign Demo_Class fan-out targets = "
                        + foreignDemoClassSimpleNames + ".",
                smellyEligibleSignatures.size() >= MIN_SMELLY_METHOD_COUNT);

        // 4. Assert foreign Demo_Class fan-out cardinality.
        assertTrue(
                "OrderProcessor must read fields of, or invoke methods on,"
                        + " at least " + MIN_FOREIGN_DEMO_CLASS_FAN_OUT
                        + " distinct other Demo_Classes across its bytecode"
                        + " (Property 9 / Requirement 5.2)."
                        + " Found " + foreignDemoClassSimpleNames.size() + ": "
                        + foreignDemoClassSimpleNames
                        + ". Smelly-method-eligible methods on OrderProcessor = "
                        + smellyEligibleSignatures + ".",
                foreignDemoClassSimpleNames.size() >= MIN_FOREIGN_DEMO_CLASS_FAN_OUT);
    }
}
