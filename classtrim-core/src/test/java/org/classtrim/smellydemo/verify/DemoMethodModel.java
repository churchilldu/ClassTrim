package org.classtrim.smellydemo.verify;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Lightweight in-package facade over a single bytecode method.
 *
 * <p>This model exists because the test classpath only carries core ASM
 * ({@code asm:9.7}); the {@code asm-tree} package ({@code ClassNode},
 * {@code MethodNode}) is not available here. The smelly-demo verification
 * harness therefore captures the subset of method-level information it needs
 * by hand during a single {@code ClassReader.accept(...)} walk.
 *
 * <p>Instances are immutable and safe to share across test methods.
 */
final class DemoMethodModel {

    /** Bytecode access flags (mask of {@code Opcodes.ACC_*}). */
    final int access;

    /** Method name, e.g. {@code "<init>"}, {@code "computeOrderTotalWithTax"}. */
    final String name;

    /** Method descriptor, e.g. {@code "(Ljava/lang/String;)V"}. */
    final String descriptor;

    /**
     * Annotation descriptors visible at runtime, e.g. {@code "Ljava/lang/Override;"}.
     * Never {@code null}; may be empty.
     */
    final List<String> visibleAnnotationDescriptors;

    /**
     * The "real" instructions of this method body, in original order. Labels,
     * line numbers, and frames are not represented because the harness's
     * pattern-matching helpers do not care about them.
     */
    final List<DemoInsn> instructions;

    DemoMethodModel(int access,
                    String name,
                    String descriptor,
                    List<String> visibleAnnotationDescriptors,
                    List<DemoInsn> instructions) {
        this.access = access;
        this.name = Objects.requireNonNull(name, "name");
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
        this.visibleAnnotationDescriptors = Collections.unmodifiableList(visibleAnnotationDescriptors);
        this.instructions = Collections.unmodifiableList(instructions);
    }

    @Override
    public String toString() {
        return name + descriptor;
    }
}
