package org.refactor.model;

import org.apache.commons.lang3.SerializationUtils;
import org.objectweb.asm.ClassReader;
import org.refactor.common.DataSet;
import org.refactor.common.Threshold;
import org.refactor.util.FileUtils;
import org.refactor.visitor.ClassVisitor;
import org.refactor.visitor.CouplingVisitor;

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

    /**
     * Getter and Setter
     **/

    private void parseInvocation(String classFilePath) {
        try (InputStream classFileInputStream = Files.newInputStream(Paths.get(classFilePath))) {
            ClassReader classReader = new ClassReader(classFileInputStream);
            classReader.accept(new CouplingVisitor(this), 0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void parse(String classFilePath) {
        try (InputStream classFileInputStream = Files.newInputStream(Paths.get(classFilePath))) {
            ClassReader classReader = new ClassReader(classFileInputStream);
            classReader.accept(new ClassVisitor(this), 0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public void start() {
        String[] allClassFiles = FileUtils.getAllClassFiles(dataSet.getPath());
        Arrays.stream(allClassFiles).forEach(this::parse);
        Arrays.stream(allClassFiles).forEach(this::parseInvocation);
    }

    /**
     * Custom method
     **/

    public JavaClass getOrCreateClass(String className) {
        return this.getClass(className).orElseGet(
                () -> {
                    JavaClass clazz = new JavaClass(className, this);
                    classList.add(clazz);
                    return clazz;
                }
        );
    }

    public JavaClass createClass(String className) {
        JavaClass clazz = new JavaClass(className, this);
        classList.add(clazz);
        return clazz;
    }

    public Optional<JavaMethod> getMethod(String className, String methodName, String descriptor) {
        return this.getClass(className).flatMap(c -> c.getMethod(methodName, descriptor));
    }

    public Optional<JavaMethod> getMethodRecursively(String owner, String methodName, String descriptor) {
        Optional<JavaClass> c = this.getClass(owner);
        while (c.isPresent()) {
            Optional<JavaMethod> method = c.get().getMethod(methodName, descriptor);
            if (method.isPresent()) {
                return method;
            }
            c = c.get().getSuperClass();
        }

        return Optional.empty();
    }

    public boolean contain(String className) {
        return this.getClass(className).isPresent();
    }

    public Optional<JavaClass> getClass(String className) {
        Iterator<JavaClass> iterator = classList.iterator();
        return classList.parallelStream()
                .filter(c -> className.equals(c.getName()))
                .findFirst();
    }

    public List<JavaClass> getClassCanRefactor() {
        if (classToRefactor == null) {
            classToRefactor = this.classList.parallelStream()
                    .filter(JavaClass::canRefactor)
                    .collect(Collectors.toList());
        }

        return classToRefactor;
    }

    public List<JavaMethod> getMethodsCanRefactor() {
        if (methodsToRefactor == null) {
            methodsToRefactor = this.classList.parallelStream()
                    .map(JavaClass::getDeclaredMethods)
                    .flatMap(List::parallelStream)
                    .filter(JavaMethod::canRefactor)
                    .collect(Collectors.toList());
        }

        return methodsToRefactor;
    }

    public Threshold getThreshold() {
        return this.dataSet.getThreshold();
    }

}