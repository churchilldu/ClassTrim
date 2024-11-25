package org.example.visitor;

import org.apache.commons.lang3.StringUtils;
import org.example.model.JavaClass;
import org.example.model.JavaMethod;
import org.example.model.JavaPackage;
import org.example.model.JavaProject;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

import java.util.HashSet;
import java.util.Set;

public class ClassVisitor extends org.objectweb.asm.ClassVisitor {
    private final Set<String> privateFieldNameSet = new HashSet<>();
    private final JavaProject project;
    private JavaClass cls;

    public ClassVisitor(JavaProject project) {
        super(Opcodes.ASM9);
        this.project = project;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        // Filter non-public class

        cls = project.getOrCreateClass(name);

        int lastDotIndex = name.lastIndexOf('/');
        if (lastDotIndex > 0) {
            String packageName = name.substring(0, lastDotIndex);
            JavaPackage pack = project.getOrCreatePackage(packageName);

            pack.addClass(cls);
        }

        if (project.contain(superName)) {
            JavaClass superClass = project.getOrCreateClass(superName);
            cls.setSuperClass(superClass);
        }
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        if ((access & Opcodes.ACC_PRIVATE) != 0) {
            privateFieldNameSet.add(name);
        }

        return super.visitField(access, name, descriptor, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        if (!"<init>".equals(name)) {
            JavaMethod method = project.getOrCreateMethod(cls, name, descriptor);
            if ((access & Opcodes.ACC_PUBLIC) == 0 || isGetterOrSetter(name)) {
                method.setCanRefactor(false);
            }

            cls.addDeclaredMethod(method);

            return new MethodVisitor(project, cls, method);
        }

        return null;
    }


    private boolean isGetterOrSetter(String methodName) {
        for (String field : privateFieldNameSet) {
            if (StringUtils.equalsIgnoreCase("get" + field, methodName)
                    || StringUtils.equalsIgnoreCase("set" + field, methodName)
                    || StringUtils.equalsIgnoreCase("is" + field, methodName)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
    }
}
