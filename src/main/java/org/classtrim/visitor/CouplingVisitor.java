package org.classtrim.visitor;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.*;
import org.classtrim.model.JavaClass;
import org.classtrim.model.JavaMethod;
import org.classtrim.model.JavaProject;
import org.classtrim.util.ASMUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Predicate;

/**
 * Bind class with their super class and interface.
 * Register class's field type.
 * class's declaring methods' argument's type, return type and exception.
 */
public class CouplingVisitor extends ClassVisitor {
    private final JavaProject project;
    private JavaClass clazz;
    private JavaMethod method;

    private final Logger logger = LoggerFactory.getLogger(CouplingVisitor.class);

    public CouplingVisitor(JavaProject project) {
        super(Opcodes.ASM9);
        this.project = project;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.project.findClass(name).ifPresent(c -> {
            this.clazz = c;
            this.setSuperClass(c, superName);
            for (String anInterface : interfaces) {
                this.addInterface(c, anInterface);
            }
        });
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        this.registerFieldType(descriptor);
        return super.visitField(access, name, descriptor, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        clazz.findMethod(name, descriptor).ifPresent(
                m -> {
                    this.method = m;
                    this.registerMethodSignature(descriptor);
                    this.registerExceptions(exceptions);
                });

        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }

    private void registerFieldType(String descriptor) {
        String className = ASMUtils.getFieldType(descriptor).getInternalName();
        if (!ASMUtils.isPrimitiveType(className) && !ASMUtils.isFromJava(className)) {
            project.findClass(className).ifPresentOrElse(clazz::registerFieldType,
                    () -> clazz.registerFieldType(new JavaClass(className, null)));
        }
    }


    private void setSuperClass(JavaClass clazz, String className) {
        project.findClass(className).ifPresentOrElse(clazz::setSuperClass,
                () -> clazz.setSuperClass(new JavaClass(className, null)));
    }

    private void addInterface(JavaClass clazz, String interfaceName) {
        project.findClass(interfaceName).ifPresentOrElse(clazz::addInterface,
                () -> clazz.addInterface(new JavaClass(interfaceName, null)));
    }

    private void registerMethodSignature(String descriptor) {
        ASMUtils.getMethodSignatureType(descriptor)
                .stream()
                .filter(Predicate.not(ASMUtils::isPrimitiveType))
                .map(Type::getInternalName)
                .filter(Predicate.not(ASMUtils::isFromJava))
                .forEach(c -> project.findClass(c).ifPresentOrElse(method::registerCoupling,
                        () -> method.registerCoupling(new JavaClass(c, null))));
    }

    private void registerExceptions(String[] exceptions) {
        if (exceptions == null) return;
        for (String exception : exceptions) {
            if (!ASMUtils.isFromJava(exception)) {
                project.findClass(exception).ifPresentOrElse(e -> method.registerCoupling(e),
                        () -> method.registerCoupling(new JavaClass(exception, null)));
            }
        }
    }

}
