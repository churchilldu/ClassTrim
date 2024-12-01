package org.refactor.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.objectweb.asm.Opcodes;

public class JavaMethod extends JavaObject {
    private final JavaClass clazz;
    private final String descriptor;
    private int access;
    private boolean isOverride;
    private boolean isGetterOrSetter;
    private int complexity = 0;

    public JavaMethod(JavaClass clazz, String name, String descriptor) {
        super(name);
        this.clazz = clazz;
        this.descriptor = descriptor;
    }

    public boolean canRefactor() {
        return (access & Opcodes.ACC_PUBLIC) != 0
                && !isGetterOrSetter
                && !isOverride;
    }

    public JavaClass getCls() {
        return clazz;
    }

    public int getComplexity() {
        return complexity;
    }

    public void setComplexity(int complexity) {
        this.complexity = complexity;
    }

    public void setAccess(int access) {
        this.access = access;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public void setGetterOrSetter(boolean getterOrSetter) {
        isGetterOrSetter = getterOrSetter;
    }

    public void setOverride(boolean override) {
        isOverride = override;
    }

    @Override
    public boolean equals(Object o) {
        return new EqualsBuilder()
                .appendSuper(super.equals(o))
                .append(this.clazz, ((JavaMethod) o).getCls())
                .append(this.descriptor, ((JavaMethod) o).getDescriptor())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(this.clazz)
                .append(this.descriptor)
                .toHashCode();
    }

    @Override
    public String toString() {
        return this.getName();
    }

}
