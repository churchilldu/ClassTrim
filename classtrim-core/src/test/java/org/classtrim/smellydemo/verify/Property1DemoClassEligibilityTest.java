package org.classtrim.smellydemo.verify;

import org.junit.Test;
import org.objectweb.asm.Opcodes;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Property 1: Demo_Class structural eligibility.
 *
 * <p>For every compiled type in {@code org.classtrim.demo.ecommerce}, asserts
 * the type is {@code public}, top-level (not an inner / nested type),
 * non-{@code abstract}, not an {@code interface}, and not an {@code enum}.
 *
 * <p>The test enumerates the universe of Demo_Classes via
 * {@link SmellyDemoLoader#loadAllDemoClassModels()} and applies each
 * structural predicate as a separate assertion so any failure points to the
 * exact offending class and rule.
 *
 * <p><b>Validates: Requirements 3.1, 3.2, 3.3, 3.4</b>
 */
public class Property1DemoClassEligibilityTest {

    @Test
    public void everyDemoClassIsPublicTopLevelConcreteClass() {
        Map<String, DemoClassModel> byName = SmellyDemoLoader.loadAllDemoClassModels();

        // Sanity: harness should always surface the full inventory before we
        // assert per-class structural rules. If this fails, run
        // `mvn -pl smelly-demo -am compile` first.
        assertEquals(
                "Expected exactly 8 Demo_Classes to verify structural eligibility against",
                8,
                byName.size());

        for (Map.Entry<String, DemoClassModel> entry : byName.entrySet()) {
            String simpleName = entry.getKey();
            DemoClassModel model = entry.getValue();
            int access = model.access;

            // Requirement 3.1: public
            assertNotEquals(
                    "Demo_Class `" + simpleName + "` must be declared public (ACC_PUBLIC)",
                    0,
                    access & Opcodes.ACC_PUBLIC);

            // Requirement 3.2: top-level (no enclosing class, not inner/nested)
            assertFalse(
                    "Demo_Class `" + simpleName + "` must be a top-level type (no InnerClasses entry)",
                    model.isInnerOrNestedType);

            // Requirement 3.3: non-abstract
            assertEquals(
                    "Demo_Class `" + simpleName + "` must not be abstract (ACC_ABSTRACT must be unset)",
                    0,
                    access & Opcodes.ACC_ABSTRACT);

            // Requirement 3.4: not an interface
            assertEquals(
                    "Demo_Class `" + simpleName + "` must not be an interface (ACC_INTERFACE must be unset)",
                    0,
                    access & Opcodes.ACC_INTERFACE);

            // Requirement 3.4: not an enum
            assertEquals(
                    "Demo_Class `" + simpleName + "` must not be an enum (ACC_ENUM must be unset)",
                    0,
                    access & Opcodes.ACC_ENUM);
        }
    }

    @Test
    public void everyDemoClassPassesAllEligibilityPredicatesAsConjunction() {
        // Companion check: applies the conjoined predicate so a single failure
        // names the offending class together with the full eligibility shape.
        for (DemoClassModel model : SmellyDemoLoader.loadAllDemoClassModelsAsList()) {
            int access = model.access;
            boolean isPublic = (access & Opcodes.ACC_PUBLIC) != 0;
            boolean topLevel = !model.isInnerOrNestedType;
            boolean nonAbstract = (access & Opcodes.ACC_ABSTRACT) == 0;
            boolean notInterface = (access & Opcodes.ACC_INTERFACE) == 0;
            boolean notEnum = (access & Opcodes.ACC_ENUM) == 0;

            assertTrue(
                    "Demo_Class `" + model.simpleName() + "` must satisfy every"
                            + " structural eligibility predicate"
                            + " (public=" + isPublic
                            + ", topLevel=" + topLevel
                            + ", nonAbstract=" + nonAbstract
                            + ", notInterface=" + notInterface
                            + ", notEnum=" + notEnum + ")",
                    isPublic && topLevel && nonAbstract && notInterface && notEnum);
        }
    }
}
