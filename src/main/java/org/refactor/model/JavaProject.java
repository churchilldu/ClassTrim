package org.refactor.model;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.refactor.common.Threshold;
import org.refactor.util.FileUtils;
import org.refactor.visitor.ClassVisitor;
import org.objectweb.asm.ClassReader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class JavaProject extends JavaObject {

    private final List<JavaMethod> methodList = new LinkedList<>();
    private final List<JavaClass> classList = new LinkedList<>();
    private final Threshold threshold;

    /**
     * Constructor
     **/
    public JavaProject(Threshold threshold) {
        this.threshold = threshold;
    }

    public static JavaProject load(String fileName) {
        try {
            File file = new File(fileName);
            return SerializationUtils.deserialize(new FileInputStream(file));

        } catch (IOException | ClassCastException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }
    }

    public void addSource(String projectPath) {
        for (String path : FileUtils.getAllClassFiles(projectPath)) {
            this.parseClass(path);
        }
    }

    private void parseClass(String classFilePath) {
        try (InputStream classFileInputStream = Files.newInputStream(Paths.get(classFilePath))) {
            ClassReader classReader = new ClassReader(classFileInputStream);
            classReader.accept(new ClassVisitor(this), 0);
        } catch (IOException e) {
            throw new RuntimeException(e);
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
     * Custom method
     **/

    public JavaMethod getOrCreateMethod(JavaClass clazz, String name, String descriptor) {
        return clazz.getMethod(name).orElseGet(
                () -> {
                    JavaMethod method = new JavaMethod(clazz, name, descriptor);
                    clazz.addDeclaredMethod(method);
                    this.addMethod(method);
                    return method;
                }
        );
    }

    public JavaClass getOrCreateClass(String className) {
        return this.getClass(className).orElseGet(
                () -> {
                    JavaClass clazz = new JavaClass(className);
                    this.addClass(clazz);
                    return clazz;
                }
        );
    }

    private void addClass(JavaClass cls) {
        this.classList.add(cls);
    }

    private void addMethod(JavaMethod m) {
        this.methodList.add(m);
    }

    private Optional<JavaClass> getClass(String name) {
        return classList.stream().filter(cls -> name.equals(cls.getName())).findFirst();
    }

    public boolean contain(String className) {
        return StringUtils.equals(className.substring(0, className.indexOf("/")), this.getName());
    }

    /**
     * Getter and Setter
     **/

    public List<JavaClass> getClassList() {
        return classList;
    }

    public List<JavaMethod> getMethodList() {
        return methodList;
    }

    public Threshold getThreshold() {
        return threshold;
    }
}
