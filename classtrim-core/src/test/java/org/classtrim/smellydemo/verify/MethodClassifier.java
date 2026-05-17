package org.classtrim.smellydemo.verify;

import org.objectweb.asm.Opcodes;

import java.util.List;

/**
 * Classify {@link DemoMethodModel}s against their declaring {@link DemoClassModel}.
 *
 * <p>All methods are static and side-effect-free, so the class is reusable as
 * a shared library across the {@code 8.x} property tests.
 *
 * <p>The "pure getter" / "pure setter" detectors are intentionally
 * conservative: they only match the canonical {@code ALOAD 0; GETFIELD <self>;
 * <X>RETURN} and {@code ALOAD 0; <X>LOAD; PUTFIELD <self>; RETURN} bytecode
 * shapes. Anything more elaborate (literal pushes, branches, multi-field
 * touches, etc.) does <em>not</em> qualify as a pure getter or setter.
 */
final class MethodClassifier {

    private MethodClassifier() {
        // utility
    }

    static boolean isPublic(DemoMethodModel m) {
        return (m.access & Opcodes.ACC_PUBLIC) != 0;
    }

    static boolean isStatic(DemoMethodModel m) {
        return (m.access & Opcodes.ACC_STATIC) != 0;
    }

    static boolean isConstructor(DemoMethodModel m) {
        return "<init>".equals(m.name);
    }

    static boolean isClassInitializer(DemoMethodModel m) {
        return "<clinit>".equals(m.name);
    }

    /** True iff {@code m}'s visible annotations contain {@code Ljava/lang/Override;}. */
    static boolean hasOverrideAnnotation(DemoMethodModel m) {
        for (String desc : m.visibleAnnotationDescriptors) {
            if ("Ljava/lang/Override;".equals(desc)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Body matches {@code ALOAD 0; GETFIELD <selfField>; <X>RETURN}.
     *
     * <p>"Self field" means the {@code GETFIELD}'s owner equals
     * {@code owner.internalName}. The {@link DemoMethodModel} only retains
     * "real" instructions (no labels, line-number nodes, or frames), so a
     * size-based check is sound.
     */
    static boolean isPureGetter(DemoClassModel owner, DemoMethodModel m) {
        if (isStatic(m) || isConstructor(m) || isClassInitializer(m)) {
            return false;
        }
        List<DemoInsn> insns = m.instructions;
        if (insns.size() != 3) {
            return false;
        }
        DemoInsn i0 = insns.get(0);
        DemoInsn i1 = insns.get(1);
        DemoInsn i2 = insns.get(2);
        if (i0.kind != DemoInsn.Kind.VAR
                || i0.opcode != Opcodes.ALOAD
                || i0.varIndex != 0) {
            return false;
        }
        if (i1.kind != DemoInsn.Kind.FIELD
                || i1.opcode != Opcodes.GETFIELD
                || !owner.internalName.equals(i1.owner)) {
            return false;
        }
        return isReturnOpcode(i2.opcode);
    }

    /**
     * Body matches {@code ALOAD 0; <X>LOAD k; PUTFIELD <selfField>; RETURN}.
     */
    static boolean isPureSetter(DemoClassModel owner, DemoMethodModel m) {
        if (isStatic(m) || isConstructor(m) || isClassInitializer(m)) {
            return false;
        }
        List<DemoInsn> insns = m.instructions;
        if (insns.size() != 4) {
            return false;
        }
        DemoInsn i0 = insns.get(0);
        DemoInsn i1 = insns.get(1);
        DemoInsn i2 = insns.get(2);
        DemoInsn i3 = insns.get(3);
        if (i0.kind != DemoInsn.Kind.VAR
                || i0.opcode != Opcodes.ALOAD
                || i0.varIndex != 0) {
            return false;
        }
        if (i1.kind != DemoInsn.Kind.VAR || !isLoadOpcode(i1.opcode)) {
            return false;
        }
        if (i2.kind != DemoInsn.Kind.FIELD
                || i2.opcode != Opcodes.PUTFIELD
                || !owner.internalName.equals(i2.owner)) {
            return false;
        }
        return i3.opcode == Opcodes.RETURN;
    }

    /**
     * Composite eligibility used throughout the property tests:
     * public, non-static, non-constructor, non-{@code @Override}, not a pure
     * getter or pure setter.
     */
    static boolean isSmellyMethodEligible(DemoClassModel owner, DemoMethodModel m) {
        if (!isPublic(m)) {
            return false;
        }
        if (isStatic(m)) {
            return false;
        }
        if (isConstructor(m) || isClassInitializer(m)) {
            return false;
        }
        if (hasOverrideAnnotation(m)) {
            return false;
        }
        if (isPureGetter(owner, m)) {
            return false;
        }
        if (isPureSetter(owner, m)) {
            return false;
        }
        return true;
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private static boolean isReturnOpcode(int op) {
        return op == Opcodes.IRETURN
                || op == Opcodes.LRETURN
                || op == Opcodes.FRETURN
                || op == Opcodes.DRETURN
                || op == Opcodes.ARETURN
                || op == Opcodes.RETURN;
    }

    private static boolean isLoadOpcode(int op) {
        return op == Opcodes.ILOAD
                || op == Opcodes.LLOAD
                || op == Opcodes.FLOAD
                || op == Opcodes.DLOAD
                || op == Opcodes.ALOAD;
    }
}
