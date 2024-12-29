package org.refactor.visitor;

import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
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

    private final Map<String, String> privateFields = new HashMap<>();
    private final JavaProject project;
    private JavaClass clazz;
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
        clazz = project.createClass(name);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        if (ASMUtils.isPrivate(access)) {
            privateFields.put(name, descriptor);
        }

        return super.visitField(access, name, descriptor, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        JavaMethod method = clazz.createMethod(name, descriptor);

        method.setAccess(access);
        method.setGetterOrSetter(ASMUtils.isGetterOrSetter(name, descriptor, privateFields));
        method.setOverride(ASMUtils.isOverride(superName, interfaces, name, descriptor));

        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }


    @Override
    public void visitEnd() {
        super.visitEnd();
        logger.info(clazz.toString());
    }
}
