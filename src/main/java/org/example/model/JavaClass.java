package org.example.model;


import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class JavaClass extends JavaObject{

    private JavaPackage pack;

    private JavaClass superClass;

    private List<JavaClass> extendedClass = new LinkedList<>();

    // <dependClass, weight>
    private Map<JavaClass, Integer> dependClass = new LinkedHashMap<>();

    // <derivedClass, weight>
    private Map<JavaClass, Integer> derivedClass = new LinkedHashMap<>();

    /** Constructor **/
    public JavaClass() {}

    public JavaClass(String name) {
        super(name);
    }

    /** Getter and Setter **/
    public JavaClass getSuperClass() {
        if (superClass == null) {
            return new JavaClass();
        }

        return superClass;
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

    public List<JavaClass> getExtendedClass() {
        return extendedClass;
    }

    public void addExtendedClass(JavaClass extendedClass) {
        this.extendedClass.add(extendedClass);
    }

    public void setSuperClass(JavaClass superClass) {
        this.superClass = superClass;
    }

    public JavaPackage getPackage() {
        return pack;
    }

    public void setPackage(JavaPackage pack) {
        this.pack = pack;
    }
}