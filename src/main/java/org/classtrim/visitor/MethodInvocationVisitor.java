package org.classtrim.visitor;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.classtrim.model.JavaClass;
import org.classtrim.model.JavaMethod;
import org.classtrim.model.JavaProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Method invocation and method use the global variable from other class.
 */
public class MethodInvocationVisitor extends ClassVisitor {
    private final JavaProject project;
    private JavaClass clazz;
    private JavaMethod method;

    private final Logger logger = LoggerFactory.getLogger(CouplingVisitor.class);

    public MethodInvocationVisitor(JavaProject project) {
        super(Opcodes.ASM9);
        this.project = project;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.project.findClass(name).ifPresent(c -> this.clazz = c);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        Optional<JavaMethod> m = clazz.findMethod(name, descriptor);
        if (m.isPresent()) {
            this.method = m.get();
            return new MV();
        }

        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }

    /**
     * Method Visitor, distinct from asm's.
     */
    private class MV extends MethodVisitor {
        private MV() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            super.visitFieldInsn(opcode, owner, name, descriptor);
            project.findClass(owner).ifPresentOrElse(method::registerCoupling,
                    () -> method.registerCoupling(new JavaClass(owner, null)));
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            project.findMethodRecursively(owner, name, descriptor).ifPresentOrElse(method::addInvokeMethod,
                    () -> method.addInvokeMethod(new JavaMethod(new JavaClass(owner, null), name, descriptor)));
        }

    }

}
