package org.refactor.visitor;

import org.refactor.model.JavaClass;
import org.refactor.model.JavaMethod;
import org.refactor.model.JavaProject;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.TypePath;

public class MethodVisitor extends org.objectweb.asm.MethodVisitor {
    private final JavaProject project;
    private final JavaClass cls;
    private final JavaMethod method;
    private int complexity = 0;

    protected MethodVisitor(JavaProject project, JavaClass cls, JavaMethod method) {
        super(Opcodes.ASM9);
        this.project = project;
        this.cls = cls;
        this.method = method;
    }

    @Override
    public void visitCode() {
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        // Count conditional and branching instructions
        if (opcode == Opcodes.IFEQ || opcode == Opcodes.IFNE ||
            opcode == Opcodes.IFLT || opcode == Opcodes.IFGE ||
            opcode == Opcodes.IFGT || opcode == Opcodes.IFLE ||
            opcode == Opcodes.IF_ICMPEQ || opcode == Opcodes.IF_ICMPNE ||
            opcode == Opcodes.IF_ICMPLT || opcode == Opcodes.IF_ICMPGE ||
            opcode == Opcodes.IF_ICMPGT || opcode == Opcodes.IF_ICMPLE ||
            opcode == Opcodes.IF_ACMPEQ || opcode == Opcodes.IF_ACMPNE ||
            opcode == Opcodes.GOTO) {
            complexity++;
        }
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        super.visitLookupSwitchInsn(dflt, keys, labels);
         // Each case in a lookup switch statement adds to the complexity
        complexity += labels.length;
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        super.visitTableSwitchInsn(min, max, dflt, labels);
          // Each case in a switch statement adds to the complexity
        complexity += labels.length;
    }

    @Override
    public void visitLabel(Label label) {
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        return super.visitAnnotation(descriptor, visible);
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
        return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
    }


    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        if ("<init>".equals(name)
                || !project.contain(owner)
                || owner.contains("$")) {
            return;
        }

        JavaClass classCall = project.getOrCreateClass(owner);
        JavaMethod invokeMethod = project.getOrCreateMethod(classCall, name, descriptor);
        cls.addInvokeMethod(invokeMethod);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        super.visitFieldInsn(opcode, owner, name, descriptor);
    }

    @Override
    public void visitEnd() {
        // Apply the formula: V(G) = E - N + 2P
        // Assume P = 1 for this method
        method.setComplexity(complexity);

        super.visitEnd();
    }
}
