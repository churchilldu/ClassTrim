package org.classtrim.model;

import lombok.Getter;
import lombok.Setter;
import org.classtrim.util.ASMUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class JavaMethod extends JavaObject {
    @Getter
    private final JavaClass clazz;
    @Getter
    private final String descriptor;
    @Setter
    private int access;
    @Setter
    private boolean isGetterOrSetter;
    /**
     * Signature includes:
     * Argument's type
     * Return type
     * Exception
     * Global variable
     */
    private final List<JavaClass> coupling = new ArrayList<>();
    private final List<JavaMethod> invokedMethods = new ArrayList<>();

    public JavaMethod(JavaClass clazz, String name, String descriptor) {
        super(name);
        this.clazz = clazz;
        this.descriptor = descriptor;
    }

    private Boolean canRefactor = null;
    public boolean canRefactor() {
        if (this.canRefactor == null) {
            this.canRefactor = this.clazz != null
                    && this.clazz.canRefactor()
                    && ASMUtils.isPublic(access)
                    && !ASMUtils.isAbstract(access)
                    && !ASMUtils.isConstructor(this.getName())
                    && !isGetterOrSetter
                    && !isOverride();
        }
        return this.canRefactor;
    }

    private boolean isOverride() {
        JavaClass c = this.clazz;
        while (c.getSuperClass().isPresent()) {
            if (c.getSuperClass().get().getProject() == null
                    || c.getSuperClass().get().getDeclaredMethods().isEmpty()) {
                return ASMUtils.isOverride(c.getName(), this.getName(), this.getDescriptor());
            } else {
                c = c.getSuperClass().get();
                for (JavaMethod method : c.getDeclaredMethods()) {
                    if (method.equals(this)) {
                        return true;
                    }
                }
            }
        }

        for (JavaClass anInterface : c.getInterfaces()) {
            if (anInterface.getProject() == null
                    || anInterface.getDeclaredMethods().isEmpty()) {
                return ASMUtils.isOverride(c.getName(), this.getName(), this.getDescriptor());
            } else {
                for (JavaMethod method : anInterface.getDeclaredMethods()) {
                    if (method.equals(this)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public void registerCoupling(JavaClass c) {
        this.coupling.add(c);
    }

    public void addInvokeMethod(JavaMethod method) {
        this.invokedMethods.add(method);
    }

    public List<JavaMethod> getInvokeMethods() {
        return Collections.unmodifiableList(invokedMethods);
    }

    public List<JavaClass> getCoupling() {
        return Collections.unmodifiableList(coupling);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o instanceof JavaMethod) {
            JavaMethod m = (JavaMethod) o;
            return m.getName().equals(this.getName())
                    && m.getClazz().equals(this.getClazz())
                    && m.getDescriptor().equals(this.descriptor);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getName(), clazz, descriptor);
    }


    @Override
    /**
     * Get the method signature.
     * @return The method signature. e.g. "method(Type1, Type2)"
     */
    public String toString() {
//        return this.clazz.toString() + "." + this.getName();
        return ASMUtils.methodToString(this.getName(), this.getDescriptor());
    }

}
