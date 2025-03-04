package org.refactor.model;


import org.objectweb.asm.Type;
import org.refactor.util.ASMUtils;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class JavaClass extends JavaObject {
    private final JavaProject project;
    private int access;
    private final List<JavaClass> fieldsType = new ArrayList<>();
    private JavaClass superClass;
    private final List<JavaClass> interfaces = new ArrayList<>();
    private final List<JavaMethod> declaredMethods = new ArrayList<>();

    public JavaClass(String name, JavaProject project) {
        super(name);
        this.project = project;
    }

    public Optional<JavaMethod> findMethod(String methodName, String descriptor) {
        return declaredMethods.stream().filter(m ->
                methodName.equals(m.getName()) && descriptor.equals(m.getDescriptor())
        ).findFirst();
    }

    public List<JavaMethod> getDeclaredMethods() {
        return Collections.unmodifiableList(declaredMethods);
    }

    private List<JavaMethod> fixedMethods;
    public List<JavaMethod> getFixedMethods() {
        if (this.fixedMethods == null) {
        this.fixedMethods = this.getDeclaredMethods().stream()
                .filter(Predicate.not(JavaMethod::canRefactor))
                .collect(Collectors.toUnmodifiableList());
        }
        return this.fixedMethods;
    }


    private Boolean canRefactor = null;

    public boolean canRefactor() {
        if (this.canRefactor == null) {
            this.canRefactor = this.project != null
                    && ASMUtils.isPublic(access)
                    && !ASMUtils.isAbstract(access)
                    && !ASMUtils.isEnum(access)
                    && !ASMUtils.isInnerClass(this.getName())
                    && !ASMUtils.isInterface(access);
        }
        return this.canRefactor;
    }

    @Override
    public String toString() {
        return Type.getObjectType(this.getName()).getClassName();
    }

    public void setAccess(int access) {
        this.access = access;
    }

    public JavaMethod createMethod(String name, String descriptor) {
        JavaMethod method = new JavaMethod(this, name, descriptor);
        declaredMethods.add(method);
        return method;
    }

    public List<JavaClass> getFieldsType() {
        return Collections.unmodifiableList(fieldsType);
    }

    public void registerFieldType(JavaClass dependency) {
        this.fieldsType.add(dependency);
    }

    public void addInterface(JavaClass anInterface) {
        this.interfaces.add(anInterface);
    }

    public Optional<JavaClass> getSuperClass() {
        return Optional.ofNullable(superClass);
    }

    public void setSuperClass(JavaClass superClass) {
        this.superClass = superClass;
    }

    public List<JavaClass> getInterfaces() {
        return Collections.unmodifiableList(interfaces);
    }

    public JavaProject getProject() {
        return project;
    }

    public boolean isInherited(JavaClass parent) {
        JavaClass c = this;
        while (c.getSuperClass().isPresent()) {
            c = c.getSuperClass().get();
            if (c.equals(parent)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof JavaClass) {
            JavaClass c = (JavaClass) o;
            return Objects.equals(this.project, c.getProject())
                    && this.getName().equals(c.getName());
        }

        return false;
    }
}