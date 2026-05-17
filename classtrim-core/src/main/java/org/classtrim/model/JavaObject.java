package org.classtrim.model;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Objects;

@Getter
public abstract class JavaObject implements Serializable {
    private static final long serialVersionUID = 1423480912791753005L;

    private final String name;

    public JavaObject(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof JavaObject) {
            return StringUtils.equals(name, ((JavaObject) o).getName());
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
