package org.classtrim.smellydemo.verify;

import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Count bytecode-level field accesses, method invocations, and {@code LDC}
 * constant pushes inside a {@link DemoMethodModel} body.
 *
 * <p>Counts are grouped by the bytecode owner internal name (e.g.
 * {@code "org/classtrim/demo/ecommerce/Order"}). Callers can run the result
 * through {@link SmellyDemoLoader#simpleNameFromInternalName(String)} to get
 * a simple name when needed.
 *
 * <p>All methods are static and side-effect-free.
 */
final class BytecodeAccessCounter {

    private BytecodeAccessCounter() {
        // utility
    }

    /**
     * Count {@link Opcodes#GETFIELD} instructions in {@code m}, grouped by
     * owner internal name.
     */
    static Map<String, Integer> getFieldsByOwner(DemoMethodModel m) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (DemoInsn insn : m.instructions) {
            if (insn.kind == DemoInsn.Kind.FIELD && insn.opcode == Opcodes.GETFIELD) {
                out.merge(insn.owner, 1, Integer::sum);
            }
        }
        return out;
    }

    /**
     * Count {@link Opcodes#PUTFIELD} instructions in {@code m}, grouped by
     * owner internal name.
     */
    static Map<String, Integer> putFieldsByOwner(DemoMethodModel m) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (DemoInsn insn : m.instructions) {
            if (insn.kind == DemoInsn.Kind.FIELD && insn.opcode == Opcodes.PUTFIELD) {
                out.merge(insn.owner, 1, Integer::sum);
            }
        }
        return out;
    }

    /**
     * Combined {@code GETFIELD + PUTFIELD} count per owner internal name.
     */
    static Map<String, Integer> fieldAccessesByOwner(DemoMethodModel m) {
        Map<String, Integer> out = new LinkedHashMap<>(getFieldsByOwner(m));
        putFieldsByOwner(m).forEach((k, v) -> out.merge(k, v, Integer::sum));
        return out;
    }

    /**
     * Count {@code INVOKEVIRTUAL / INVOKEINTERFACE / INVOKESPECIAL /
     * INVOKESTATIC} instructions in {@code m}, grouped by owner internal name.
     * {@code INVOKEDYNAMIC} is intentionally excluded since it has no static
     * owner; the demo classes do not use it.
     */
    static Map<String, Integer> invokesByOwner(DemoMethodModel m) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (DemoInsn insn : m.instructions) {
            if (insn.kind != DemoInsn.Kind.METHOD) {
                continue;
            }
            int op = insn.opcode;
            if (op == Opcodes.INVOKEVIRTUAL
                    || op == Opcodes.INVOKEINTERFACE
                    || op == Opcodes.INVOKESPECIAL
                    || op == Opcodes.INVOKESTATIC) {
                out.merge(insn.owner, 1, Integer::sum);
            }
        }
        return out;
    }

    /**
     * Combined "field-reads + method-invocations targeting owner X". This is
     * the metric used by the Feature Envy property: a method enviously reaches
     * into a foreign Demo_Class when this combined count is strictly greater
     * for a foreign owner than for the declaring class.
     */
    static Map<String, Integer> fieldReadsPlusInvokesByOwner(DemoMethodModel m) {
        Map<String, Integer> out = new LinkedHashMap<>(getFieldsByOwner(m));
        invokesByOwner(m).forEach((k, v) -> out.merge(k, v, Integer::sum));
        return out;
    }

    /**
     * Return the constants pushed via {@code LDC} in method body order. Useful
     * for asserting Shotgun_Surgery literal pushes such as {@code 0.0825} or
     * {@code 0.92}.
     */
    static List<Object> ldcConstants(DemoMethodModel m) {
        List<Object> out = new ArrayList<>();
        for (DemoInsn insn : m.instructions) {
            if (insn.kind == DemoInsn.Kind.LDC) {
                out.add(insn.constant);
            }
        }
        return out;
    }

    /**
     * Convenience: does {@code m} push the exact double literal {@code value}
     * via an {@code LDC} instruction? Compares with {@link Double#compare} so
     * NaN/-0 oddities behave deterministically.
     */
    static boolean containsLdcDouble(DemoMethodModel m, double value) {
        for (Object cst : ldcConstants(m)) {
            if (cst instanceof Double && Double.compare((Double) cst, value) == 0) {
                return true;
            }
        }
        return false;
    }
}
