package org.refactor.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.refactor.util.ASMUtils;

import java.util.*;

public class JavaMethod extends JavaObject {
    private final JavaClass clazz;
    private final String descriptor;
    private int access;
    private boolean isGetterOrSetter;
    /**
     * Signature includes:
     * Argument's type
     * Return type
     * Exception
     */
    private final List<JavaClass> signature = new ArrayList<>();
    private final List<JavaMethod> invokedMethods = new ArrayList<>();

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
                && !isOverride();
    }

    private boolean isOverride() {
        JavaClass c = this.clazz;
        while (c.getSuperClass().isPresent()) {
            if (c.getSuperClass().get().getProject() != null) {
                c = c.getSuperClass().get();
                for (JavaMethod method : c.getDeclaredMethods()) {
                    if (method.equals(this)) {
                        return true;
                    }
                }
            } else {
                return ASMUtils.isOverride(c.getName(), this.getName(), this.getDescriptor());
            }
        }

        for (JavaClass anInterface : c.getInterfaces()) {
            if (anInterface.getProject() != null) {
                for (JavaMethod method : anInterface.getDeclaredMethods()) {
                    if (method.equals(this)) {
                        return true;
                    }
                }
            } else {
                return ASMUtils.isOverride(c.getName(), this.getName(), this.getDescriptor());
            }
        }

        return false;
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

    public void registerCoupling(JavaClass c) {
        this.signature.add(c);
    }

    public void addInvokeMethod(JavaMethod method) {
        this.invokedMethods.add(method);
    }

    public List<JavaMethod> getInvokeMethods() {
        return Collections.unmodifiableList(invokedMethods);
    }

    public List<JavaClass> getSignature() {
        return Collections.unmodifiableList(signature);
    }


    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof JavaMethod) {
            JavaMethod m = (JavaMethod) o;
            return new EqualsBuilder()
                    .appendSuper(super.equals(o))
                    .append(this.clazz, m.getClazz())
                    .append(this.descriptor, m.getDescriptor())
                    .isEquals();
        }

        return false;
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
