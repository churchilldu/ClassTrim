package org.example.visitor;

import org.example.model.JavaClass;
import org.example.model.JavaMethod;
import org.example.model.JavaProject;
import org.example.util.ASMUtils;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ClassVisitor extends org.objectweb.asm.ClassVisitor {
    private final Logger logger = LoggerFactory.getLogger("");

    private final Map<String, Type> privateFields = new HashMap<>();
    private final JavaProject project;
    private JavaClass cls;

    public ClassVisitor(JavaProject project) {
        super(Opcodes.ASM9);
        this.project = project;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        cls = project.getOrCreateClass(name);
        logger.info("parse class: {}", cls.toString());
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        if ((access & Opcodes.ACC_PRIVATE) != 0) {
            privateFields.put(name, Type.getObjectType(descriptor));
        }

        return super.visitField(access, name, descriptor, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        if (!"<init>".equals(name)) {
            JavaMethod method = project.getOrCreateMethod(cls, name, descriptor);
            method.setAccess(access);
            method.setGetterOrSetter(ASMUtils.isGetterOrSetter(method, privateFields));

            return new MethodVisitor(project, cls, method);
        }

        return null;
    }


    @Override
    public void visitEnd() {
        super.visitEnd();
    }
}
