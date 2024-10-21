package org.example.model;

import java.util.LinkedList;
import java.util.List;

public class JavaPackage extends JavaObject {
    private List<JavaClass> classList = new LinkedList<>();

    /** Constructor **/
    public JavaPackage() {
    }

    public JavaPackage(String name) {
        super(name);
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
}
