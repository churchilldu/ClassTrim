package org.refactor.model;


import org.objectweb.asm.Type;

import java.util.*;
import java.util.stream.Collectors;

public class JavaClass extends JavaObject {
    private final List<JavaMethod> declaredMethods = new ArrayList<>();
    private final Set<String> dependencies = new HashSet<>();

    public JavaClass(String name) {
        super(name);
    }

    public Optional<JavaMethod> getMethod(String methodName, String descriptor) {
        return declaredMethods.stream().filter(m ->
                methodName.equals(m.getName()) && descriptor.equals(m.getDescriptor())
        ).findFirst();
    }

    public List<JavaMethod> getInvokedMethods() {
        return this.declaredMethods.stream()
                .map(JavaMethod::getInvokeMethods)
                .flatMap(Set::parallelStream)
                .collect(Collectors.toList());
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

    public void addDependency(String name) {
        this.dependencies.add(name);
    }

    public int getExternalCbo() {
        return this.dependencies.size();
    }
}