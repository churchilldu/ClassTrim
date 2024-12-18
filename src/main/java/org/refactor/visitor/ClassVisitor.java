package org.refactor.visitor;

import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.refactor.model.JavaClass;
import org.refactor.model.JavaMethod;
import org.refactor.model.JavaProject;
import org.refactor.util.ASMUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ClassVisitor extends org.objectweb.asm.ClassVisitor {
    private final Logger logger = LoggerFactory.getLogger(ClassVisitor.class);

    private final Map<String, Type> privateFields = new HashMap<>();
    private final JavaProject project;
    private JavaClass cls;
    private String superName;
    private String[] interfaces;

    public ClassVisitor(JavaProject project) {
        super(Opcodes.ASM9);
        this.project = project;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.superName = superName;
        this.interfaces = interfaces;
        cls = project.getOrCreateClass(name);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        if (ASMUtils.isPrivate(access)) {
            privateFields.put(name, Type.getType(descriptor));
        }

        return super.visitField(access, name, descriptor, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        JavaMethod method = project.getOrCreateMethod(cls, name, descriptor);

        method.setAccess(access);
        method.setGetterOrSetter(ASMUtils.isGetterOrSetter(name, descriptor, privateFields));
        method.setOverride(ASMUtils.isOverride(project.getUrlCL(), superName, interfaces, name, descriptor));

        return new MethodVisitor(project, method);
    }


    @Override
    public void visitEnd() {
        super.visitEnd();
        logger.info("parse class: {}", cls.toString());
    }
}
