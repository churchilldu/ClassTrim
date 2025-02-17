package org.refactor.model;

import org.apache.commons.lang3.SerializationUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.refactor.common.DataSet;
import org.refactor.common.Threshold;
import org.refactor.util.ASMUtils;
import org.refactor.util.FileUtils;
import org.refactor.visitor.ClazzVisitor;
import org.refactor.visitor.CouplingVisitor;
import org.refactor.visitor.MethodInvocationVisitor;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class JavaProject extends JavaObject {
    private final List<JavaClass> classList = new ArrayList<>();
    private final DataSet dataSet;
    private List<JavaClass> classToRefactor;
    private List<JavaMethod> methodsToRefactor;

    public JavaProject(DataSet dataSet) {
        super(dataSet.getName());
        this.dataSet = dataSet;
    }

    public static JavaProject load(String fileName) {
        try {
            File file = new File(fileName);
            return SerializationUtils.deserialize(new FileInputStream(file));

        } catch (IOException | ClassCastException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }
    }

    public void save(String fileName) {
        try {
            File file = new File(fileName);
            if (file.isDirectory()) {
                throw new IllegalArgumentException("cannot overwrite directory");
            }
            FileOutputStream str = new FileOutputStream(file);
            SerializationUtils.serialize(this, str);
            str.close();
        } catch (IOException | ClassCastException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }
    }

    public void start() {
        String[] allClassFiles = FileUtils.getAllClassFiles(dataSet.getPath());
        Arrays.stream(allClassFiles).forEach(this::parseMethods);
        Arrays.stream(allClassFiles).forEach(this::parseInheritance);
        Arrays.stream(allClassFiles).forEach(this::parseInvocation);
    }

    private void parseMethods(String classFilePath) {
        this.parse(classFilePath, new ClazzVisitor(this));
    }

    private void parseInheritance(String classFilePath) {
        this.parse(classFilePath, new CouplingVisitor(this));
    }

    private void parseInvocation(String classFilePath) {
        this.parse(classFilePath, new MethodInvocationVisitor(this));
    }

    private void parse(String filePath, ClassVisitor visitor) {
        try (InputStream classFileInputStream = Files.newInputStream(Paths.get(filePath))) {
            ClassReader classReader = new ClassReader(classFileInputStream);
            classReader.accept(visitor, 0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public JavaClass createClass(String className) {
        JavaClass clazz = new JavaClass(className, this);
        classList.add(clazz);
        return clazz;
    }

    public Optional<JavaMethod> getMethod(String className, String methodName, String descriptor) {
        return this.findClass(className).flatMap(c -> c.findMethod(methodName, descriptor));
    }

    public Optional<JavaMethod> findMethodRecursively(String owner, String methodName, String descriptor) {
        Optional<JavaClass> c = this.findClass(owner);
        while (c.isPresent()) {
            if (c.get().getProject() == null && c.get().getDeclaredMethods().isEmpty()) {
                ASMUtils.loadMethodsToClass(c.get());
            }
            Optional<JavaMethod> method = c.get().findMethod(methodName, descriptor);
            if (method.isPresent()) {
                return method;
            }
            c = c.get().getSuperClass();
        }

        return Optional.empty();
    }

    public Optional<JavaClass> findClass(String className) {
        return classList.stream().filter(c -> className.equals(c.getName()))
                .findFirst();
    }

    public List<JavaClass> getClassCanRefactor() {
        if (classToRefactor == null) {
            classToRefactor = this.classList.stream()
                    .filter(JavaClass::canRefactor)
                    .collect(Collectors.toList());
        }
        return Collections.unmodifiableList(classToRefactor);
    }

    // todo Should I include all methods from class that can be refactored.
    public List<JavaMethod> getMethodsToRefactor() {
        if (methodsToRefactor == null) {
            methodsToRefactor = this.getClassCanRefactor().stream()
                    .map(JavaClass::getDeclaredMethods)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
        }
        return Collections.unmodifiableList(methodsToRefactor);
    }

    public Threshold getThreshold() {
        return this.dataSet.getThreshold();
    }

    public List<JavaClass> getClassList() {
        return Collections.unmodifiableList(classList);
    }

}