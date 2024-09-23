package org.example.model;


import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class JavaClass extends Object implements Serializable {
    private static final long serialVersionUID = 8781885987620700898L;

    private String name;

    private JavaPackage pack;

    private JavaClass superClass;
    private List<JavaClass> extendedClass = new LinkedList<>();

    // <dependClass, weight>
    private Map<JavaClass, Integer> dependClass = new LinkedHashMap<>();
    // <derivedClass, weight>
    private Map<JavaClass, Integer> derivedClass = new LinkedHashMap<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof JavaClass) {
            return StringUtils.equals(this.getName(), ((JavaClass) o).getName());
        }

        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 31). // two randomly chosen prime numbers
                // if deriving: appendSuper(super.hashCode()).
                        append(name).
                toHashCode();
    }

    /** Constructor **/
    public JavaClass() {
    }

    public JavaClass(String name) {
        this.name = name;
    }

    /** Getter and Setter **/
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

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

    public void setSuperClass(String superClass) {
        this.superClass = new JavaClass(superClass);
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

/**
 * LinkedList
 * construct JavaClass then groupingBy to <JavaPackage, List<JavaClass>>
 *    construct JavaPackage and  JavaProject.
 */