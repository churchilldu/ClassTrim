package org.example.model;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public class JavaPackage implements Serializable {
    private static final long serialVersionUID = 8781885987620700898L;

    private String name;
    private List<JavaClass> classList = new LinkedList<>();

    /** Constructor **/
    public JavaPackage() {
    }

    public JavaPackage(String name) {
        this.name = name;
    }

    public JavaPackage(JavaPackage pack) {
        this(pack.getName());
    }

    public List<JavaClass> getClassList() {
        return classList;
    }

    public void setClassList(List<JavaClass> classList) {
        this.classList = classList;
    }

    public void addClass(JavaClass cls) {
        this.classList.add(cls);
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o instanceof JavaPackage) {
            return this.name.equals(((JavaPackage) o).getName());
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
}
