package org.example.model;


import java.util.*;

public class JavaClass extends JavaObject{

    private JavaPackage pack;
    private JavaClass superClass;

    private List<JavaClass> extendedClass = new LinkedList<>();

    // <dependClass, weight>
    private Map<JavaClass, Integer> dependClass = new LinkedHashMap<>();

    // <derivedClass, weight>
    private Map<JavaClass, Integer> derivedClass = new LinkedHashMap<>();

    private List<JavaMethod> declaredMethodList = new LinkedList<>();
    private List<JavaMethod> invokeMethodList = new LinkedList<>();


    /** Constructor **/
    public JavaClass() {}

    public JavaClass(String name) {
        super(name);
    }

    /**
     * Getter and Setter
     **/
    public Optional<JavaClass> getSuperClass() {
        return Optional.ofNullable(superClass);
    }

    public Map<JavaClass, Integer> getDerivedClass() {
        return derivedClass;
    }

    public void setDerivedClass(Map<JavaClass, Integer> derivedClass) {
        this.derivedClass = derivedClass;
    }

    public Map<JavaClass, Integer> getDependClass() {
        return dependClass;
    }

    public void setDependClass(Map<JavaClass, Integer> dependClass) {
        this.dependClass = dependClass;
    }
    public void addDependClass(JavaClass dependClass) {
        this.dependClass.put(dependClass, this.dependClass.getOrDefault(dependClass, 0) + 1);
        dependClass.getDerivedClass().put(this, dependClass.getDerivedClass().getOrDefault(dependClass, 0) + 1);
    }

    public List<JavaClass> getExtendedClass() {
        return extendedClass;
    }

    public void addExtendedClass(JavaClass extendedClass) {
        this.extendedClass.add(extendedClass);
    }

    public void setSuperClass(JavaClass superClass) {
        this.superClass = superClass;
        superClass.addExtendedClass(this);
    }

    public JavaPackage getPackage() {
        return pack;
    }

    public void setPackage(JavaPackage pack) {
        this.pack = pack;
    }

    public void addDeclaredMethod(JavaMethod method) {
        this.declaredMethodList.add(method);
        method.setClass(this);
    }

    public void addInvokeMethod(JavaMethod method) {
        this.invokeMethodList.add(method);
        this.addDependClass(method.getCls());
    }

    public List<JavaMethod> getDeclaredMethodList() {
        return declaredMethodList;
    }

    public List<JavaMethod> getInvokeMethodList() {
        return invokeMethodList;
    }

    public void setInvokeMethodList(List<JavaMethod> invokeMethodList) {
        this.invokeMethodList = invokeMethodList;
    }

    public void setDeclaredMethodList(List<JavaMethod> declaredMethodList) {
        this.declaredMethodList = declaredMethodList;
    }

    public JavaMethod getMethodByName(String name, String descriptor) {
        for (JavaMethod method : this.declaredMethodList) {
            if (method.equals(name, descriptor)) {
                return method;
            }
        }

        return null;
    }

}