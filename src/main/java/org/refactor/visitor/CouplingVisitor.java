package org.refactor.visitor;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.*;
import org.refactor.model.JavaClass;
import org.refactor.model.JavaMethod;
import org.refactor.model.JavaProject;
import org.refactor.util.ASMUtils;

import java.util.Optional;
import java.util.function.Predicate;

public class CouplingVisitor extends ClassVisitor {
    private final JavaProject project;
    private JavaClass clazz;
    private JavaMethod method;

    public CouplingVisitor(JavaProject project) {
        super(Opcodes.ASM9);
        this.project = project;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.project.getClass(name).ifPresent(c -> {
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
        Optional<JavaMethod> m = clazz.getMethod(name, descriptor);
        if (m.isPresent()) {
            this.method = m.get();
            this.registerMethodSignature(descriptor);
            this.registerExceptions(exceptions);
            return new MV();
        }

        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }

    private void setSuperClass(JavaClass clazz, String className) {
        if (!ASMUtils.isFromJava(className)) {
            project.getClass(className).ifPresentOrElse(clazz::setSuperClass,
                    () -> clazz.setSuperClass(new JavaClass(className, null)));
        }
    }

    private void addInterface(JavaClass clazz, String interfaceName) {
        if (!ASMUtils.isFromJava(interfaceName)) {
            project.getClass(interfaceName).ifPresentOrElse(clazz::addInterface,
                    () -> clazz.addInterface(new JavaClass(interfaceName, null)));
        }
    }

    private void registerMethodSignature(String descriptor) {
        ASMUtils.getMethodSignatureType(descriptor).stream()
                .filter(Predicate.not(ASMUtils::isFromJava))
                .forEach(c -> project.getClass(c).ifPresentOrElse(method::registerCoupling,
                        () -> method.registerCoupling(new JavaClass(c, null))));
    }

    private void registerExceptions(String[] exceptions) {
        if (exceptions == null) return;
        for (String exception : exceptions) {
            if (!ASMUtils.isFromJava(exception)) {
                project.getClass(exception).ifPresentOrElse(e -> method.registerCoupling(e),
                        () -> method.registerCoupling(new JavaClass(exception, null)));
            }
        }
    }

    private void registerFieldType(String descriptor) {
        String className = Type.getObjectType(descriptor).getInternalName();
        if (!ASMUtils.isFromJava(className)) {
            project.getClass(className).ifPresentOrElse(clazz::registerFieldType,
                    () -> clazz.registerFieldType(new JavaClass(className, null))
            );
        }
    }

    private class MV extends MethodVisitor {
        protected MV() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            project.getMethodRecursively(owner, name, descriptor).ifPresentOrElse(method::addInvokeMethod,
                    () -> method.addInvokeMethod(new JavaMethod(new JavaClass(owner, null), name, descriptor)));
        }

    }

}
