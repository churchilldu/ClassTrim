package org.classtrim.visitor;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.classtrim.model.JavaClass;
import org.classtrim.model.JavaMethod;
import org.classtrim.model.JavaProject;
import org.classtrim.util.ASMUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Class visitor (clazz not class is to distinct from asm's).
 *
 */
public class ClazzVisitor extends ClassVisitor {
    private final Logger logger = LoggerFactory.getLogger(ClazzVisitor.class);

    private final Map<String, String> privateFields = new HashMap<>();
    private final JavaProject project;
    private JavaClass clazz;

    public ClazzVisitor(JavaProject project) {
        super(Opcodes.ASM9);
        this.project = project;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        clazz = project.createClass(name);
        clazz.setAccess(access);
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

        if (!ASMUtils.isConstructor(name)) {
            method.setGetterOrSetter(ASMUtils.isGetterOrSetter(name, descriptor, privateFields));
        }

        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        logger.info(clazz.toString());
    }
}
