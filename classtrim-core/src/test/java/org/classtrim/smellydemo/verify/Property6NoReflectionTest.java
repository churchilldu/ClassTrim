package org.classtrim.smellydemo.verify;

import org.junit.Test;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Property 6: Smells use no reflection or {@code toString} indirection.
 *
 * <p>For every method declared on a Demo_Class in
 * {@code org.classtrim.demo.ecommerce}, asserts that the method's bytecode
 * contains:
 * <ul>
 *   <li>no {@code INVOKE*} instruction whose owner is in
 *       {@code java.lang.reflect.*} (e.g. {@code java.lang.reflect.Method},
 *       {@code java.lang.reflect.Field}, {@code java.lang.reflect.Array}, ...);</li>
 *   <li>no {@code INVOKE*} instruction whose owner is in
 *       {@code java.lang.invoke.MethodHandle*} (covers
 *       {@code MethodHandle}, {@code MethodHandles}, {@code MethodHandles$Lookup},
 *       {@code MethodHandleProxies}, ...);</li>
 *   <li>no {@code INVOKEVIRTUAL toString()Ljava/lang/String;} instruction
 *       whose receiver type is another Demo_Class.</li>
 * </ul>
 *
 * <p>Note: invoking {@code toString()} on {@code java.lang.String},
 * {@code java.lang.StringBuilder}, {@code java.lang.Object},
 * {@code java.time.Instant}, etc. is fine — only invocations whose owner is
 * itself a Demo_Class (per
 * {@link SmellyDemoLoader#isDemoClassInternalName(String)}) are forbidden,
 * because they would represent smells expressed through {@code toString}
 * parsing rather than through real field reads or method invocations.
 *
 * <p><b>Validates: Requirements 3.10</b>
 */
public class Property6NoReflectionTest {

    /** Internal-name prefix for {@code java.lang.reflect.*}. */
    private static final String REFLECT_OWNER_PREFIX = "java/lang/reflect/";

    /**
     * Internal-name prefix for {@code java.lang.invoke.MethodHandle*} —
     * deliberately matches {@code MethodHandle}, {@code MethodHandles},
     * {@code MethodHandles$Lookup}, {@code MethodHandleProxies}, etc.
     */
    private static final String METHOD_HANDLE_OWNER_PREFIX = "java/lang/invoke/MethodHandle";

    @Test
    public void noDemoMethodInvokesReflectionOrMethodHandleApis() {
        List<String> violations = new ArrayList<>();

        for (DemoClassModel cls : SmellyDemoLoader.loadAllDemoClassModelsAsList()) {
            for (DemoMethodModel m : cls.methods) {
                for (DemoInsn insn : m.instructions) {
                    if (insn.kind != DemoInsn.Kind.METHOD) {
                        continue;
                    }
                    String owner = insn.owner;
                    if (owner == null) {
                        continue;
                    }
                    if (owner.startsWith(REFLECT_OWNER_PREFIX)
                            || owner.startsWith(METHOD_HANDLE_OWNER_PREFIX)) {
                        violations.add(formatViolation(cls, m, insn,
                                "reflection or MethodHandle API invocation"));
                    }
                }
            }
        }

        assertTrue(
                "Demo_Classes must not invoke reflection or MethodHandle APIs."
                        + " Offending instructions:\n  - "
                        + String.join("\n  - ", violations),
                violations.isEmpty());
    }

    @Test
    public void noDemoMethodInvokesToStringOnAnotherDemoClass() {
        List<String> violations = new ArrayList<>();

        for (DemoClassModel cls : SmellyDemoLoader.loadAllDemoClassModelsAsList()) {
            for (DemoMethodModel m : cls.methods) {
                for (DemoInsn insn : m.instructions) {
                    if (insn.kind != DemoInsn.Kind.METHOD) {
                        continue;
                    }
                    if (insn.opcode != Opcodes.INVOKEVIRTUAL) {
                        continue;
                    }
                    if (!"toString".equals(insn.memberName)) {
                        continue;
                    }
                    if (!"()Ljava/lang/String;".equals(insn.memberDescriptor)) {
                        continue;
                    }
                    if (!SmellyDemoLoader.isDemoClassInternalName(insn.owner)) {
                        // toString() on String / StringBuilder / Object / Instant / ... is fine
                        continue;
                    }
                    violations.add(formatViolation(cls, m, insn,
                            "INVOKEVIRTUAL toString() on another Demo_Class"));
                }
            }
        }

        assertTrue(
                "Demo_Classes must not invoke toString() on another Demo_Class."
                        + " Offending instructions:\n  - "
                        + String.join("\n  - ", violations),
                violations.isEmpty());
    }

    private static String formatViolation(DemoClassModel cls,
                                          DemoMethodModel m,
                                          DemoInsn insn,
                                          String reason) {
        return cls.simpleName() + "." + m.name + m.descriptor
                + " contains " + reason + ": "
                + "opcode=" + insn.opcode
                + " owner=" + insn.owner
                + " name=" + insn.memberName
                + " descriptor=" + insn.memberDescriptor;
    }
}
