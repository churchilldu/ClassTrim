package org.refactor.visitor;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.refactor.model.JavaProject;

public class CouplingVisitor extends ClassVisitor {
    private final JavaProject project;

    public CouplingVisitor(JavaProject project) {
        super(Opcodes.ASM9);
        this.project = project;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        return new MethodVisitor(Opcodes.ASM9) {
            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            }
        };
    }

}
