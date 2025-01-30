package org.refactor.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.refactor.util.ASMUtils;

import java.util.HashSet;
import java.util.Set;

public class JavaMethod extends JavaObject {
    private final JavaClass clazz;
    private final String descriptor;
    private int access;
    private boolean isOverride;
    private boolean isGetterOrSetter;
    // Arguments' type and return type
    // Check is count of number or times
    private final Set<JavaClass> signatureType = new HashSet<>();
    private final Set<JavaMethod> invokedMethods = new HashSet<>();

    public JavaMethod(JavaClass clazz, String name, String descriptor) {
        super(name);
        this.clazz = clazz;
        this.descriptor = descriptor;
    }

    public boolean canRefactor() {
        return this.clazz != null
                && this.clazz.canRefactor()
                && ASMUtils.isPublic(access)
                && !ASMUtils.isAbstract(access)
                && !ASMUtils.isConstructor(this.getName())
                && !isGetterOrSetter
                && !isOverride;
    }

    public JavaClass getClazz() {
        return clazz;
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

    public void addDependency(JavaClass cls) {
        this.signatureType.add(cls);
    }

    public void addInvokeMethod(JavaMethod method) {
        this.invokedMethods.add(method);
    }

    public Set<JavaMethod> getInvokeMethods() {
        return invokedMethods;
    }

    public Set<JavaClass> getSignatureType() {
        return signatureType;
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
