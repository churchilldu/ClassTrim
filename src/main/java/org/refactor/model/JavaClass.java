package org.refactor.model;


import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JavaClass extends JavaObject {
    private final List<JavaMethod> declaredMethods = new ArrayList<>();

    public JavaClass(String name) {
        super(name);
    }

    public Optional<JavaMethod> getMethod(String methodName, String descriptor) {
        return declaredMethods.stream().filter(m ->
                methodName.equals(m.getName()) && descriptor.equals(m.getDescriptor())
        ).findFirst();
    }

    public void addDeclaredMethod(JavaMethod method) {
        this.declaredMethods.add(method);
    }

    public List<JavaMethod> getDeclaredMethods() {
        return declaredMethods;
    }

    @Override
    public String toString() {
        return Type.getObjectType(this.getName()).getClassName();
    }

}