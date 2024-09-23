package org.example.model;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class JavaProject implements Serializable {
    private static final long serialVersionUID = 6640945341857405094L;

    private List<JavaPackage> packageList = new LinkedList<>();
    private List<JavaClass> classList = new LinkedList<>();
    private String name;

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
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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
        this.packageList.add(new JavaPackage(pack));
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
        if (StringUtils.isEmpty(name)) {
            return null;
        }

        for (JavaPackage pack : this.packageList) {
            if (name.equals(pack.getName())) {
                return pack;
            }
        }

        return null;
    }

    public JavaClass getClassByName(String name) {
        if (StringUtils.isEmpty(name)) {
            return null;
        }

        for (JavaClass cls : this.classList) {
            if (name.equals(cls.getName())) {
                return cls;
            }
        }

        return null;
    }

    public String diff(JavaProject that) {
        if (that == null) {
            return null;
        }

        StringBuilder diff = new StringBuilder(this.getName());

        this.getPackageList().forEach(
                pack -> {
                    JavaPackage thatPack = that.getPackage(pack);
                    diff.append(pack.getName());
                    diff.append("\n");

                    diff.append(pack.getClassList().size());
                    diff.append(" -> ");
                    diff.append(thatPack.getClassList().size());

                    diff.append(System.lineSeparator());
                    diff.append("\n");
                    diff.append("--------\n");

                    CollectionUtils.retainAll(thatPack.getClassList(), pack.getClassList()).forEach(
                            c -> {
                                diff.append(c.getName());
                                diff.append("\n");
                            }
                    );
                    CollectionUtils.removeAll(thatPack.getClassList(), pack.getClassList()).forEach(
                            c -> {
                                diff.append("+ ");
                                diff.append(c.getName());
                                diff.append("\n");
                            }
                    );
                    CollectionUtils.removeAll(pack.getClassList(), thatPack.getClassList()).forEach(
                            c -> {
                                diff.append("- ");
                                diff.append(c.getName());
                                diff.append("\n");
                            }
                    );
                    diff.append("\n");
                }
        );

        return diff.toString();
    }

    public void addSource(String path) {
        if (path == null || path.isEmpty()) {
            return;
        }

        File direcotry = new File(path);
        if (direcotry.exists() && direcotry.isDirectory() && direcotry.listFiles() != null && direcotry.listFiles().length > 0) {
            this.setName(direcotry.listFiles()[0].getName());

            this.getFilePath(direcotry.listFiles()).forEach(
                    clsPath -> getClassInfo(clsPath, this)
            );
        }

    }

    private void getClassInfo(String classFilePath, JavaProject project) {
        try (InputStream classFileInputStream = new FileInputStream(classFilePath)) {
            // Use ASM ClassReader to read the class file
            ClassReader classReader = new ClassReader(classFileInputStream);
            classReader.accept(new ClassVisitor(Opcodes.ASM9) {
                JavaClass cls;

                @Override
                public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                    super.visit(version, access, name, signature, superName, interfaces);
                    // Filter non-public class

                    cls = project.getOrCreateClass(name);

                    int lastDotIndex = name.lastIndexOf('/');
                    if (lastDotIndex != -1) {
                        String packageName = name.substring(0, lastDotIndex);
                        JavaPackage pack = project.getOrCreatePackage(packageName);

                        // package --- class
                        pack.addClass(cls);
                        cls.setPackage(pack);
                    }


                    // Filter class not in project
                    if (superName.startsWith(project.getName())) {
                        JavaClass superClass = project.getOrCreateClass(superName);
                        // superClass --- class
                        cls.setSuperClass(superClass);
                        superClass.addExtendedClass(cls);
                    }
                }


                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override
                        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                            // Filter class own method
                            // Method didn't belong to this project
                            // Method is a constructor
                            if (owner.equals(cls.getName()) || !owner.startsWith(project.getName()) || owner.contains("$")) {
                                return;
                            }

                            JavaClass dependClass = project.getOrCreateClass(owner);
                            // Update edge between class
                            cls.getDependClass().put(dependClass, cls.getDependClass().getOrDefault(dependClass, 0) + 1);
                            dependClass.getDerivedClass().put(cls, dependClass.getDerivedClass().getOrDefault(dependClass, 0) + 1);
                        }
                    };
                }

            }, 0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
//            BufferedOutputStream str = new BufferedOutputStream(Files.newOutputStream(file.toPath()));
            FileOutputStream str = new FileOutputStream(file);
            SerializationUtils.serialize(this, str);
            str.close();
        } catch (IOException | ClassCastException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }
    }

}
