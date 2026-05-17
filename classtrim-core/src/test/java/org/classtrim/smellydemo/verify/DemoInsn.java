package org.classtrim.smellydemo.verify;

/**
 * Minimal in-package instruction record used by the smelly-demo verification
 * harness. Only the bytecode-level information the {@code 8.x} property tests
 * actually inspect is captured.
 *
 * <p>Categorized via {@link Kind} so callers can do
 * {@code if (insn.kind == Kind.GETFIELD)} without consulting {@code Opcodes}.
 *
 * <p>Instances are immutable.
 */
final class DemoInsn {

    enum Kind {
        /** A {@code VAR} instruction such as {@code ALOAD} / {@code ILOAD}. */
        VAR,
        /** A {@code FIELD} instruction; one of {@link #GETFIELD}/{@link #PUTFIELD}/{@code GETSTATIC}/{@code PUTSTATIC}. */
        FIELD,
        /** A {@code METHOD} invocation instruction. */
        METHOD,
        /** An {@code LDC} constant push. */
        LDC,
        /** Any other "real" opcode the harness does not classify in detail. */
        OTHER
    }

    /** Underlying ASM opcode (one of the {@code Opcodes.*} constants). */
    final int opcode;

    /** Kind tag derived from the opcode. */
    final Kind kind;

    // ----- VAR -----
    /** Local-variable index for {@link Kind#VAR} instructions; {@code -1} otherwise. */
    final int varIndex;

    // ----- FIELD / METHOD -----
    /** Owner internal name for {@link Kind#FIELD} or {@link Kind#METHOD}. */
    final String owner;
    /** Field or method name for {@link Kind#FIELD} or {@link Kind#METHOD}. */
    final String memberName;
    /** Field or method descriptor for {@link Kind#FIELD} or {@link Kind#METHOD}. */
    final String memberDescriptor;
    /**
     * For {@link Kind#METHOD}: {@code true} iff the invoked owner is an
     * interface (i.e. {@code INVOKEINTERFACE}). For other kinds: {@code false}.
     */
    final boolean isInterface;

    // ----- LDC -----
    /** Pushed constant for {@link Kind#LDC} instructions; {@code null} otherwise. */
    final Object constant;

    private DemoInsn(int opcode,
                     Kind kind,
                     int varIndex,
                     String owner,
                     String memberName,
                     String memberDescriptor,
                     boolean isInterface,
                     Object constant) {
        this.opcode = opcode;
        this.kind = kind;
        this.varIndex = varIndex;
        this.owner = owner;
        this.memberName = memberName;
        this.memberDescriptor = memberDescriptor;
        this.isInterface = isInterface;
        this.constant = constant;
    }

    static DemoInsn var(int opcode, int varIndex) {
        return new DemoInsn(opcode, Kind.VAR, varIndex, null, null, null, false, null);
    }

    static DemoInsn field(int opcode, String owner, String name, String descriptor) {
        return new DemoInsn(opcode, Kind.FIELD, -1, owner, name, descriptor, false, null);
    }

    static DemoInsn method(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        return new DemoInsn(opcode, Kind.METHOD, -1, owner, name, descriptor, isInterface, null);
    }

    static DemoInsn ldc(Object constant) {
        // ASM uses LDC opcode (18) for all variants in the visitor API.
        return new DemoInsn(org.objectweb.asm.Opcodes.LDC, Kind.LDC, -1, null, null, null, false, constant);
    }

    static DemoInsn other(int opcode) {
        return new DemoInsn(opcode, Kind.OTHER, -1, null, null, null, false, null);
    }
}
