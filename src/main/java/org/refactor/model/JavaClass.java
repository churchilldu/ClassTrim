package org.refactor.model;


import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class JavaClass extends JavaObject {
    private final List<JavaMethod> declaredMethodList = new ArrayList<>();
    private final List<JavaMethod> invokeMethodList = new ArrayList<>();
    private final Set<String> dependencies = new HashSet<>();

    public JavaClass(String name) {
        super(name);
    }

    public Optional<JavaMethod> getMethod(String methodName, String descriptor) {
        return declaredMethodList.stream().filter(m ->
                methodName.equals(m.getName()) && descriptor.equals(m.getDescriptor())
        ).findFirst();
    }

    public void addDeclaredMethod(JavaMethod method) {
        this.declaredMethodList.add(method);
    }

    public void addInvokeMethod(JavaMethod method) {
        this.invokeMethodList.add(method);
    }

    public List<JavaMethod> getDeclaredMethodList() {
        return declaredMethodList;
    }

    public List<JavaMethod> getInvokeMethodList() {
        return invokeMethodList;
    }

    @Override
    public String toString() {
        return Type.getObjectType(this.getName()).getClassName();
    }

    public void addDependency(String name) {
        this.dependencies.add(name);
    }

    public int getOuterCbo() {
        return this.dependencies.size();
    }
}