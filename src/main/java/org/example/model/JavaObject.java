package org.example.model;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;

public class JavaObject implements Serializable {
    private static final long serialVersionUID = 1423480912791753005L;

    private String name;

    public JavaObject(String name) {
        this.name = name;
    }

    public JavaObject() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof JavaObject) {
            return StringUtils.equals(this.getName(), ((JavaObject) o).getName());
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

    @Override
    public String toString() {
        return this.getName().replaceAll("/", ".");
    }
}
