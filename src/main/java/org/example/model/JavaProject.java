package org.example.model;

import org.apache.commons.lang3.SerializationUtils;
import org.example.visitor.MyClassVisitor;
import org.objectweb.asm.ClassReader;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class JavaProject extends JavaObject {
    private final List<JavaMethod> methodList = new LinkedList<>();
    private List<JavaPackage> packageList = new LinkedList<>();
    private List<JavaClass> classList = new LinkedList<>();
    private List<JavaMethod> methodToRefactor = new LinkedList<>();

    /**
     * Constructor
     **/
    public JavaProject() {
    }

    public static JavaProject load(String fileName) {
        try {
            File file = new File(fileName);
            return SerializationUtils.deserialize(new FileInputStream(file));

        } catch (IOException | ClassCastException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }
    }

    /**
     * Getter and Setter
     **/
    public List<JavaPackage> getPackageList() {
        return packageList;
    }

    public void setPackageList(List<JavaPackage> packageList) {
        this.packageList = packageList;
    }

    public List<JavaClass> getClassList() {
        return classList;
    }

    public void setClassList(List<JavaClass> classList) {
        this.classList = classList;
    }

    public List<JavaMethod> getMethodList() {
        return methodList;
    }

    /**
     * Custom method
     **/

    public JavaPackage getOrCreatePackage(String packageName) {
        JavaPackage pack = this.getPackageByName(packageName);
        if (pack == null) {
            pack = new JavaPackage(packageName);
            this.addPackage(pack);
        }

        return pack;
    }

    public JavaMethod getOrCreateMethod(JavaClass cls, String methodName, String descriptor) {
        return this.getOrCreateMethod(cls.getName(), methodName, descriptor);
    }

    public JavaMethod getOrCreateMethod(String className, String methodName, String descriptor) {
        JavaClass cls = this.getOrCreateClass(className);
        return Optional.ofNullable(cls.getMethodByName(methodName, descriptor)).orElseGet(
                () -> {
                    return new JavaMethod(methodName, descriptor);
                }
        );
    }

    public JavaClass getOrCreateClass(String className) {
        JavaClass cls = this.getClassByName(className);
        if (cls == null) {
            cls = new JavaClass(className);
            this.addClass(cls);
        }

        return cls;
    }

    private void addClass(JavaClass cls) {
        this.classList.add(cls);
    }

    private void addPackage(JavaPackage pack) {
        this.packageList.add(pack);
    }

    public JavaPackage getPackage(JavaPackage pack) {
        for (JavaPackage p : this.packageList) {
            if (p.equals(pack)) {
                return p;
            }
        }

        return null;
    }

    public JavaPackage getPackageByName(String name) {
        for (JavaPackage pack : this.packageList) {
            if (name.equals(pack.getName())) {
                return pack;
            }
        }

        return null;
    }

    public JavaClass getClassByName(String name) {
        for (JavaClass cls : this.classList) {
            if (name.equals(cls.getName())) {
                return cls;
            }
        }

        return null;
    }


    public void addSource(String path) {
        if (path == null || path.isEmpty()) {
            return;
        }

        File direcotry = new File(path);
        if (direcotry.exists() && direcotry.isDirectory() && direcotry.listFiles() != null && direcotry.listFiles().length > 0) {
            this.setName(direcotry.listFiles()[0].getName());

            this.getFilePath(direcotry.listFiles()).forEach(clsPath -> getClassInfo(clsPath, this));
        }

    }

    private void getClassInfo(String classFilePath, JavaProject project) {
        try (InputStream classFileInputStream = new FileInputStream(classFilePath)) {
            // Use ASM ClassReader to read the class file
            ClassReader classReader = new ClassReader(classFileInputStream);
            classReader.accept(new MyClassVisitor(project), 0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean contain(String className) {
        return className.startsWith(this.getName());
    }

    private List<String> getFilePath(File[] files) {
        if (files == null) {
            return null;
        }

        List<String> pathList = new ArrayList<>();

        for (File f : files) {
            if (f.isDirectory()) {
                pathList.addAll(getFilePath(f.listFiles()));
            } else if (f.isFile() && f.getName().endsWith(".class") && !f.getName().contains("$")) {
                pathList.add(f.getAbsolutePath());
            }
        }


        return pathList;
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

    public List<JavaMethod> getMethodToRefactor() {
        // Filter out method override method and getter setter
        this.classList.parallelStream().forEach(
                cls -> {
                    Set<JavaMethod> overrideMethodSet = new HashSet<>();
                    overrideMethodSet.addAll(this.getOverrideMethodSet(overrideMethodSet, cls));
                    overrideMethodSet.addAll(cls.getDerivedClass().keySet().parallelStream()
                            .flatMap(c -> c.getDeclaredMethodList().parallelStream())
                            .filter(JavaMethod::CanRefactor)
                            .collect(Collectors.toSet()));


                    cls.getDeclaredMethodList().forEach(
                            method -> {
                                if (method.CanRefactor()) {
                                    if (isOverrideMethod(overrideMethodSet, method)) {
                                        method.setCanRefactor(false);
                                    }
                                }
                            }
                    );
                }
        );

        this.methodToRefactor = this.classList.parallelStream()
                .flatMap(c -> c.getDeclaredMethodList().parallelStream())
                .filter(JavaMethod::CanRefactor)
                .collect(Collectors.toList());

        return methodToRefactor;
    }

    private Set<JavaMethod> getOverrideMethodSet(Set<JavaMethod> overrideMethodSet, JavaClass cls) {
        cls.getSuperClass().ifPresent(
                superClass -> {
                    overrideMethodSet.addAll(superClass.getDeclaredMethodList());

                    getOverrideMethodSet(overrideMethodSet, superClass);
                }
        );

        return overrideMethodSet;

    }

    private boolean isOverrideMethod(Set<JavaMethod> methods, JavaMethod who) {
        for (JavaMethod method : methods) {
            if (method.equals(who.getName(), who.getDescriptor())) {
                return true;
            }
        }

        return false;
    }
}
