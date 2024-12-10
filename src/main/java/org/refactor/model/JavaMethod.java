package org.refactor.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.refactor.util.ASMUtils;

import java.util.HashSet;
import java.util.Set;

public class JavaMethod extends JavaObject {
    private final JavaClass clazz;
    private final String descriptor;
    private final Set<JavaMethod> invokedMethods = new HashSet<>();
    private final Set<String> externalMethods = new HashSet<>();
    private final Set<String> externalClasses = new HashSet<>();
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
        return ASMUtils.isPublic(access)
                && !ASMUtils.isAbstract(access)
                && !isGetterOrSetter
                && !isOverride;
    }

    public JavaClass getClazz() {
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

    public void addInvokeMethod(JavaMethod method) {
        this.invokedMethods.add(method);
    }

    public void addExternalMethod(String signature) {
        this.externalMethods.add(signature);
    }

    public void addExternalClass(String name) {
        this.externalClasses.add(name);
    }

    public Set<JavaMethod> getInvokeMethods() {
        return invokedMethods;
    }

    public Set<String> getExternalMethods() {
        return externalMethods;
    }

    public Set<String> getExternalClasses() {
        return externalClasses;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (!(o instanceof JavaMethod)) {
            return false;
        }

        JavaMethod m = (JavaMethod) o;
        return new EqualsBuilder()
                .appendSuper(super.equals(o))
                .append(this.clazz, m.getClazz())
                .append(this.descriptor, m.getDescriptor())
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
