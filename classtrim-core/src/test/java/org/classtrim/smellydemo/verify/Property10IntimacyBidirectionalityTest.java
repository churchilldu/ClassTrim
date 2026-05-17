package org.classtrim.smellydemo.verify;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Property 10: Inappropriate Intimacy bidirectionality.
 *
 * <p>For each Inappropriate_Intimacy_Pair {@code (A, B)} declared in the
 * design (drawn from {@code {(Order, Customer), (Invoice, Order)}}), assert
 * that at least one Smelly_Method on {@code A} reads a field of {@code B} or
 * invokes a non-getter/non-setter method of {@code B}, and that at least one
 * Smelly_Method on {@code B} reads a field of {@code A} or invokes a
 * non-getter/non-setter method of {@code A}.
 *
 * <p>Smelly_Method eligibility is delegated to
 * {@link MethodClassifier#isSmellyMethodEligible(DemoClassModel, DemoMethodModel)}.
 * Field reads are detected via
 * {@link BytecodeAccessCounter#getFieldsByOwner(DemoMethodModel)}; foreign
 * method invocations are detected by walking instructions and resolving the
 * called method on the foreign class's own {@link DemoClassModel} so the
 * "non-getter/non-setter" predicate can be applied to the foreign method.
 *
 * <p><b>Validates: Requirements 6.1, 6.2, 6.3</b>
 */
public class Property10IntimacyBidirectionalityTest {

    /**
     * The Inappropriate_Intimacy_Pairs declared in the design. Each entry is
     * an ordered {@code (A, B)} tuple; bidirectionality is asserted for both
     * directions of each tuple.
     */
    private static final List<String[]> INTIMACY_PAIRS = Arrays.asList(
            new String[]{"Order", "Customer"},
            new String[]{"Invoice", "Order"}
    );

    @Test
    public void everyDeclaredIntimacyPairIsBidirectionallySmelly() {
        Map<String, DemoClassModel> byName = SmellyDemoLoader.loadAllDemoClassModels();

        for (String[] pair : INTIMACY_PAIRS) {
            String aName = pair[0];
            String bName = pair[1];
            DemoClassModel a = byName.get(aName);
            DemoClassModel b = byName.get(bName);

            assertNotNull(
                    "Demo_Class `" + aName + "` must be loadable for Inappropriate_Intimacy_Pair ("
                            + aName + ", " + bName + ")",
                    a);
            assertNotNull(
                    "Demo_Class `" + bName + "` must be loadable for Inappropriate_Intimacy_Pair ("
                            + aName + ", " + bName + ")",
                    b);

            // Forward edge: A → B (some Smelly_Method on A touches B)
            List<DemoMethodModel> forward = findSmellyMethodsTouchingForeign(a, b);
            assertTrue(
                    "Inappropriate_Intimacy_Pair (" + aName + ", " + bName + "): missing forward "
                            + "intimacy edge `" + aName + " -> " + bName + "`. Expected at least "
                            + "one Smelly_Method on `" + aName + "` to read a field of `" + bName
                            + "` or invoke a non-getter/non-setter method of `" + bName
                            + "`, but the bytecode of all eligible Smelly_Methods on `" + aName
                            + "` does neither.",
                    !forward.isEmpty());

            // Back edge: B → A (some Smelly_Method on B touches A)
            List<DemoMethodModel> backward = findSmellyMethodsTouchingForeign(b, a);
            assertTrue(
                    "Inappropriate_Intimacy_Pair (" + aName + ", " + bName + "): missing back "
                            + "intimacy edge `" + bName + " -> " + aName + "`. Expected at least "
                            + "one Smelly_Method on `" + bName + "` to read a field of `" + aName
                            + "` or invoke a non-getter/non-setter method of `" + aName
                            + "`, but the bytecode of all eligible Smelly_Methods on `" + bName
                            + "` does neither.",
                    !backward.isEmpty());
        }
    }

    /**
     * Enumerate every Smelly_Method-eligible method on {@code owner} whose
     * bytecode either reads a field of {@code foreign} or invokes a
     * non-getter/non-setter method of {@code foreign}.
     */
    private static List<DemoMethodModel> findSmellyMethodsTouchingForeign(
            DemoClassModel owner, DemoClassModel foreign) {
        List<DemoMethodModel> hits = new ArrayList<>();
        for (DemoMethodModel m : owner.methods) {
            if (!MethodClassifier.isSmellyMethodEligible(owner, m)) {
                continue;
            }
            if (methodTouchesForeign(m, foreign)) {
                hits.add(m);
            }
        }
        return hits;
    }

    /**
     * @return {@code true} when {@code m}'s bytecode reads at least one field
     *         whose declared owner is {@code foreign.internalName}, or invokes
     *         at least one method on {@code foreign} that is <em>not</em> a
     *         pure getter or pure setter on {@code foreign} per
     *         {@link MethodClassifier#isPureGetter(DemoClassModel, DemoMethodModel)}
     *         /
     *         {@link MethodClassifier#isPureSetter(DemoClassModel, DemoMethodModel)}.
     */
    private static boolean methodTouchesForeign(DemoMethodModel m, DemoClassModel foreign) {
        // 1. Direct foreign field read.
        Map<String, Integer> fieldsByOwner = BytecodeAccessCounter.getFieldsByOwner(m);
        Integer foreignFieldReads = fieldsByOwner.get(foreign.internalName);
        if (foreignFieldReads != null && foreignFieldReads > 0) {
            return true;
        }

        // 2. Invocation of a non-getter/non-setter method on the foreign class.
        // The owner-grouped count from BytecodeAccessCounter alone does not
        // tell us the called method's name+descriptor, so we walk instructions
        // and resolve each callee against the foreign class's DemoClassModel.
        Map<String, Integer> invokesByOwner = BytecodeAccessCounter.invokesByOwner(m);
        Integer foreignInvokes = invokesByOwner.get(foreign.internalName);
        if (foreignInvokes == null || foreignInvokes <= 0) {
            return false;
        }

        for (DemoInsn insn : m.instructions) {
            if (insn.kind != DemoInsn.Kind.METHOD) {
                continue;
            }
            if (!foreign.internalName.equals(insn.owner)) {
                continue;
            }
            DemoMethodModel called = findForeignMethodByNameAndDescriptor(
                    foreign, insn.memberName, insn.memberDescriptor);
            if (called == null) {
                // Could not resolve the callee on the foreign class's own
                // model (e.g., the call targets an inherited or synthetic
                // method that is not declared on `foreign`). Be conservative:
                // an unresolved foreign call is not counted as a confirmed
                // non-getter/non-setter intimacy edge.
                continue;
            }
            if (MethodClassifier.isPureGetter(foreign, called)
                    || MethodClassifier.isPureSetter(foreign, called)) {
                continue;
            }
            return true;
        }
        return false;
    }

    /**
     * Resolve a method on {@code foreign} by its bytecode-level name and
     * descriptor. Returns {@code null} when no declared method on
     * {@code foreign} matches both keys.
     */
    private static DemoMethodModel findForeignMethodByNameAndDescriptor(
            DemoClassModel foreign, String name, String descriptor) {
        for (DemoMethodModel candidate : foreign.methods) {
            if (candidate.name.equals(name) && candidate.descriptor.equals(descriptor)) {
                return candidate;
            }
        }
        return null;
    }
}
