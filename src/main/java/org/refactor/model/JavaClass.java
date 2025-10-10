package org.refactor.model;


import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.Type;
import org.refactor.util.ASMUtils;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
public class JavaClass extends JavaObject {
    @Getter
    private final JavaProject project;
    @Setter
    private int access;
    private final List<JavaClass> fieldsType = new ArrayList<>();
    @Setter
    private JavaClass superClass;
    private final List<JavaClass> interfaces = new ArrayList<>();
    private final List<JavaMethod> declaredMethods = new ArrayList<>();

    public JavaClass(String name, JavaProject project) {
        super(name);
        this.project = project;
    }

    /**
     * Find a method by its methodName and parameter types. (Return type doesn't matter)
     * @param methodName The methodName of the method to find. e.g. "method"
     * @param parameterTypes The parameter types of the method to find. e.g. "I"
     * @return An Optional containing the JavaMethod if found, otherwise empty.
     */
    public Optional<JavaMethod> findMethod(String methodName, String... parameterTypes) {
        List<JavaMethod> nameMatchedMethods = this.declaredMethods.stream()
                .filter(m -> methodName.equals(m.getName()))
                .collect(Collectors.toList());

        if (nameMatchedMethods.isEmpty()) {
            return Optional.empty();
        }

        if (nameMatchedMethods.size() == 1) {
            return Optional.of(nameMatchedMethods.get(0));
        }

        for (JavaMethod method : nameMatchedMethods) {
            String[] parameters = ASMUtils.getParameters(method.getDescriptor());
            if (parameterTypesEqual(parameterTypes, parameters)) {
                return Optional.of(method);
            }
        }
        log.error("Unmatch argument types: methodName {}, parameterTypes {}.", methodName, parameterTypes);
        return Optional.empty();
    }

    /**
     * Compare parameter types by their simple class names only
     * @param types1 First array of parameter types
     * @param types2 Second array of parameter types
     * @return true if all corresponding types have the same simple class name
     */
    private boolean parameterTypesEqual(String[] types1, String[] types2) {
        if (types1.length != types2.length) {
            return false;
        } 
        
        for (int i = 0; i < types1.length; i++) {
            String simpleName1 = getSimpleClassName(types1[i]);
            String simpleName2 = getSimpleClassName(types2[i]);
            if (!simpleName1.equals(simpleName2)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Extract simple class name from full class name
     * @param fullClassName Full class name like "java.lang.Class" or "java.util.List"
     * @return Simple class name like "Class" or "List"
     */
    private String getSimpleClassName(String fullClassName) {
        if (fullClassName == null || fullClassName.isEmpty()) {
            return fullClassName;
        }
        
        int lastDotIndex = fullClassName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return fullClassName;
        }
        
        return fullClassName.substring(lastDotIndex + 1);
    }

    /**
     * Find a method by its name and descriptor. 
     * @param methodName The name of the method to find. e.g. "method"
     * @param descriptor The descriptor of the method to find. e.g. "(I)I".
     *  A descriptor is a string representation of the method's parameter types and return type.
     *  For example, "(I)I" means a method with one int parameter and an int return type.
     *  ASM's Type.getMethodDescriptor(Type[]) can be used to get the descriptor of a method.
     * @return An Optional containing the JavaMethod if found, otherwise empty.
     */
    public Optional<JavaMethod> findMethod(String methodName, String descriptor) {
        return this.declaredMethods.stream()
                .filter(m -> methodName.equals(m.getName()) && descriptor.equals(m.getDescriptor()))
                .findFirst();
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
    /**
     * Get the class name.
     * @return The class name. e.g. "org.example.Class"
     */
    public String toString() {
        return Type.getObjectType(this.getName()).getClassName();
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

    public List<JavaClass> getInterfaces() {
        return Collections.unmodifiableList(interfaces);
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