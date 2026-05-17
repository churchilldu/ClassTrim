package org.classtrim.smellydemo.verify;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.Assert.assertTrue;

/**
 * Property 8: Feature Envy distribution and count.
 *
 * <p>Algorithmically discovers every Feature_Envy_Method (FEM) across the
 * full Demo_Class inventory by walking each Smelly_Method-eligible method's
 * bytecode and asking, for each foreign Demo_Class owner appearing in its
 * combined "field reads + method invocations" tally, whether that foreign
 * count strictly exceeds the self count. Methods qualifying against any
 * foreign Demo_Class are FEMs.
 *
 * <p>Asserts:
 * <ul>
 *     <li>The deduplicated set of FEMs (keyed by
 *         {@code (declaringClass, methodName, descriptor)}) has cardinality
 *         {@code >= 4}.</li>
 *     <li>The set of distinct declaring Demo_Classes containing at least one
 *         FEM has cardinality {@code >= 3}.</li>
 *     <li>At least 3 of the FEMs are declared on {@code OrderProcessor}.</li>
 * </ul>
 *
 * <p>Discovery is purely bytecode-driven: no hardcoded registry of method
 * names is used. The harness exposes
 * {@link BytecodeAccessCounter#fieldReadsPlusInvokesByOwner(DemoMethodModel)}
 * for the access-count metric and
 * {@link MethodClassifier#isSmellyMethodEligible(DemoClassModel, DemoMethodModel)}
 * for filtering down to candidate Smelly_Methods.
 *
 * <p><b>Validates: Requirements 4.1, 5.4</b>
 */
public class Property8FeatureEnvyDistributionTest {

    /** Simple name of the God Class against which a per-class FEM count is enforced. */
    private static final String ORDER_PROCESSOR = "OrderProcessor";

    /**
     * Property 8: cardinality, distribution, and OrderProcessor-share of the
     * Feature_Envy_Method set.
     *
     * <p><b>Validates: Requirements 4.1, 5.4</b>
     */
    @Test
    public void featureEnvyMethodSetHasRequiredCardinalityDistributionAndProcessorShare() {
        Map<String, DemoClassModel> byName = SmellyDemoLoader.loadAllDemoClassModels();

        // Discover the FEM set algorithmically. We retain three views:
        //   * the deduplicated method-level set (by declaringClass + name + descriptor)
        //   * the per-class FEM count for OrderProcessor
        //   * the per-foreign-class triple set for richer failure output
        Set<MethodKey> femMethodKeys = new LinkedHashSet<>();
        Set<String> declaringClassesWithFems = new LinkedHashSet<>();
        List<FemTriple> femTriples = new ArrayList<>();
        int orderProcessorFemCount = 0;

        for (DemoClassModel owner : byName.values()) {
            String ownerSimple = owner.simpleName();
            String ownerInternal = owner.internalName;
            boolean ownerHasFem = false;

            for (DemoMethodModel method : owner.methods) {
                if (!MethodClassifier.isSmellyMethodEligible(owner, method)) {
                    continue;
                }

                Map<String, Integer> counts =
                        BytecodeAccessCounter.fieldReadsPlusInvokesByOwner(method);
                int selfCount = counts.getOrDefault(ownerInternal, 0);

                boolean methodIsFem = false;
                for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                    String foreignInternal = entry.getKey();
                    int foreignCount = entry.getValue();
                    if (Objects.equals(foreignInternal, ownerInternal)) {
                        continue;
                    }
                    if (!SmellyDemoLoader.isDemoClassInternalName(foreignInternal)) {
                        continue;
                    }
                    if (foreignCount > selfCount) {
                        String foreignSimple =
                                SmellyDemoLoader.simpleNameFromInternalName(foreignInternal);
                        femTriples.add(new FemTriple(
                                ownerSimple, method.name, method.descriptor, foreignSimple,
                                selfCount, foreignCount));
                        methodIsFem = true;
                    }
                }

                if (methodIsFem) {
                    femMethodKeys.add(new MethodKey(ownerSimple, method.name, method.descriptor));
                    ownerHasFem = true;
                    if (ORDER_PROCESSOR.equals(ownerSimple)) {
                        orderProcessorFemCount++;
                    }
                }
            }

            if (ownerHasFem) {
                declaringClassesWithFems.add(ownerSimple);
            }
        }

        // Build a stable failure message that includes the discovered FEM set
        // so triage is straightforward when the property is violated.
        String discovered = renderDiscovered(
                femMethodKeys, declaringClassesWithFems, orderProcessorFemCount, femTriples);

        assertTrue(
                "Property 8 (cardinality): expected at least 4 Feature_Envy_Methods,"
                        + " found " + femMethodKeys.size() + ".\n" + discovered,
                femMethodKeys.size() >= 4);

        assertTrue(
                "Property 8 (distribution): expected FEMs distributed across at"
                        + " least 3 distinct declaring Demo_Classes, found "
                        + declaringClassesWithFems.size() + " ("
                        + new TreeSet<>(declaringClassesWithFems) + ").\n" + discovered,
                declaringClassesWithFems.size() >= 3);

        assertTrue(
                "Property 8 (God Class share): expected at least 3 FEMs declared"
                        + " on OrderProcessor, found " + orderProcessorFemCount + ".\n"
                        + discovered,
                orderProcessorFemCount >= 3);
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private static String renderDiscovered(Set<MethodKey> femMethodKeys,
                                           Set<String> declaringClassesWithFems,
                                           int orderProcessorFemCount,
                                           List<FemTriple> femTriples) {
        StringBuilder sb = new StringBuilder();
        sb.append("Discovered Feature_Envy_Methods (deduplicated by declaringClass+name+descriptor):\n");
        if (femMethodKeys.isEmpty()) {
            sb.append("  <none>\n");
        } else {
            List<MethodKey> sorted = new ArrayList<>(femMethodKeys);
            Collections.sort(sorted);
            for (MethodKey k : sorted) {
                sb.append("  - ").append(k).append('\n');
            }
        }
        sb.append("Distinct declaring Demo_Classes with FEMs: ")
                .append(new TreeSet<>(declaringClassesWithFems)).append('\n');
        sb.append("OrderProcessor FEM count: ").append(orderProcessorFemCount).append('\n');
        sb.append("(declaringClass, method, foreignClass) triples [self/foreign access counts]:\n");
        if (femTriples.isEmpty()) {
            sb.append("  <none>\n");
        } else {
            List<FemTriple> sortedTriples = new ArrayList<>(femTriples);
            Collections.sort(sortedTriples);
            for (FemTriple t : sortedTriples) {
                sb.append("  - ").append(t).append('\n');
            }
        }
        return sb.toString();
    }

    /** Identity key for deduplicating FEMs at the method level. */
    private static final class MethodKey implements Comparable<MethodKey> {
        final String declaringClass;
        final String name;
        final String descriptor;

        MethodKey(String declaringClass, String name, String descriptor) {
            this.declaringClass = declaringClass;
            this.name = name;
            this.descriptor = descriptor;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MethodKey)) return false;
            MethodKey other = (MethodKey) o;
            return declaringClass.equals(other.declaringClass)
                    && name.equals(other.name)
                    && descriptor.equals(other.descriptor);
        }

        @Override
        public int hashCode() {
            return Objects.hash(declaringClass, name, descriptor);
        }

        @Override
        public int compareTo(MethodKey o) {
            int c = declaringClass.compareTo(o.declaringClass);
            if (c != 0) return c;
            c = name.compareTo(o.name);
            if (c != 0) return c;
            return descriptor.compareTo(o.descriptor);
        }

        @Override
        public String toString() {
            return declaringClass + "#" + name + descriptor;
        }
    }

    /** Per-foreign-class FEM triple, used for richer failure rendering. */
    private static final class FemTriple implements Comparable<FemTriple> {
        final String declaringClass;
        final String methodName;
        final String descriptor;
        final String foreignClass;
        final int selfCount;
        final int foreignCount;

        FemTriple(String declaringClass, String methodName, String descriptor,
                  String foreignClass, int selfCount, int foreignCount) {
            this.declaringClass = declaringClass;
            this.methodName = methodName;
            this.descriptor = descriptor;
            this.foreignClass = foreignClass;
            this.selfCount = selfCount;
            this.foreignCount = foreignCount;
        }

        @Override
        public int compareTo(FemTriple o) {
            int c = declaringClass.compareTo(o.declaringClass);
            if (c != 0) return c;
            c = methodName.compareTo(o.methodName);
            if (c != 0) return c;
            c = descriptor.compareTo(o.descriptor);
            if (c != 0) return c;
            return foreignClass.compareTo(o.foreignClass);
        }

        @Override
        public String toString() {
            return "(" + declaringClass + "#" + methodName + descriptor
                    + " -> " + foreignClass
                    + " [self=" + selfCount + ", foreign=" + foreignCount + "])";
        }
    }
}
