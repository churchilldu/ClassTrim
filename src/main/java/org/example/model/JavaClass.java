package org.example.model;


import org.objectweb.asm.Type;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class JavaClass extends JavaObject {
    private final List<JavaMethod> declaredMethodList = new LinkedList<>();
    private final List<JavaMethod> invokeMethodList = new LinkedList<>();

    /**
     * Constructor
     **/

    public JavaClass(String name) {
        super(name);
    }

    /**
     * Getter and Setter
     **/
    public Optional<JavaMethod> getMethod(String name) {
        return declaredMethodList.stream().filter(m -> name.equals(m.getName())).findFirst();
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

    public String getQulifiedName() {
        return Type.getObjectType(this.getName()).getClassName();
    }

    @Override
    public String toString() {
        return this.getQulifiedName();
    }
}