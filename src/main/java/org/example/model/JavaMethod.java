package org.example.model;

import org.apache.commons.lang3.StringUtils;

import java.util.Set;

public class JavaMethod extends JavaObject {

    private final String descriptor;
    private JavaClass cls;
    private int complexity = 0;
    private boolean canRefactor = true;

    // const
    public static Set<JavaMethod> objectMethodSet;

    public JavaMethod(String name, String descriptor) {
        super(name);
        this.descriptor = descriptor;
    }

    public JavaClass getCls() {
        return cls;
    }

    public void setClass(JavaClass cls) {
        this.cls = cls;
    }

    public String getDescriptor() {
        return descriptor;
    }


    public int getComplexity() {
        return complexity;
    }

    public void setComplexity(int complexity) {
        this.complexity = complexity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof JavaMethod) {
            return StringUtils.equals(this.getName(), ((JavaMethod) o).getName())
                    && this.getCls().equals(((JavaMethod) o).getCls())
                    && StringUtils.equals(this.getDescriptor(), ((JavaMethod) o).getDescriptor());
        }

        return false;

    }

    public boolean equals(String name, String descriptor) {
        return StringUtils.equals(this.getName(), name) && StringUtils.equals(this.descriptor, descriptor);
    }

    public void setCanRefactor(boolean canRefactor) {
        this.canRefactor = canRefactor;
    }

    public boolean CanRefactor() {
        return this.canRefactor;
    }
}
