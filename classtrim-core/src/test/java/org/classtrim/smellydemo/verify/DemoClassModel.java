package org.classtrim.smellydemo.verify;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Lightweight in-package facade over a single Demo_Class.
 *
 * <p>See {@link DemoMethodModel} for the rationale (no {@code asm-tree} on the
 * test classpath). Captures only the structural information the verification
 * harness needs.
 */
final class DemoClassModel {

    /** Bytecode access flags (mask of {@code Opcodes.ACC_*}). */
    final int access;

    /** Internal name (slash-separated), e.g. {@code "org/classtrim/demo/ecommerce/Order"}. */
    final String internalName;

    /** Internal name of the super class (or {@code null} for {@code java/lang/Object}). */
    final String superInternalName;

    /** Internal names of declared interfaces; never {@code null}. */
    final List<String> interfaceInternalNames;

    /**
     * {@code true} when the {@code .class} file declares an
     * {@code InnerClasses} entry whose inner-name equals {@link #internalName}
     * (i.e. this type itself is an inner/nested class). For the demo module
     * this should always be {@code false}.
     */
    final boolean isInnerOrNestedType;

    /** All methods declared on this class (constructors, statics, instance, etc.). */
    final List<DemoMethodModel> methods;

    DemoClassModel(int access,
                   String internalName,
                   String superInternalName,
                   List<String> interfaceInternalNames,
                   boolean isInnerOrNestedType,
                   List<DemoMethodModel> methods) {
        this.access = access;
        this.internalName = Objects.requireNonNull(internalName, "internalName");
        this.superInternalName = superInternalName;
        this.interfaceInternalNames = Collections.unmodifiableList(interfaceInternalNames);
        this.isInnerOrNestedType = isInnerOrNestedType;
        this.methods = Collections.unmodifiableList(methods);
    }

    String simpleName() {
        return SmellyDemoLoader.simpleNameFromInternalName(internalName);
    }

    @Override
    public String toString() {
        return simpleName();
    }
}
