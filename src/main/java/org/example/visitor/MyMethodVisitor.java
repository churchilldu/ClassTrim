package org.example.visitor;

import org.example.model.JavaClass;
import org.example.model.JavaMethod;
import org.example.model.JavaProject;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MyMethodVisitor extends MethodVisitor {
    private final JavaProject project;
    private final JavaClass cls;
    private final JavaMethod method;
    private int nodeCount = 0;
    private int edgeCount = 0;

    protected MyMethodVisitor(JavaProject project, JavaClass cls, JavaMethod method) {
        super(Opcodes.ASM9);
        this.project = project;
        this.cls = cls;
        this.method = method;
    }

    @Override
    public void visitCode() {
        nodeCount++; // Entry point
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        edgeCount++; // Each jump instruction creates an edge
        nodeCount++; // Each target label is a node
    }

    @Override
    public void visitLabel(Label label) {
        // Labels are considered nodes
        nodeCount++;
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        /**
         * Filter out following method:
         * 1. class own method
         * 2. outside of project
         * 3. constructor
         * 4. inner class method
         */

        if ("<init>".equals(name)
                || owner.equals(cls.getName())
                || !project.contain(owner)
                || owner.contains("$")) {
            return;
        }

        JavaMethod invokeMethod = project.getOrCreateMethod(owner, name, descriptor);
        JavaClass dependClass = project.getOrCreateClass(owner);
        invokeMethod.setClass(dependClass);
        cls.addInvokeMethod(invokeMethod);
    }

    @Override
    public void visitEnd() {
        // Apply the formula: V(G) = E - N + 2P
        // Assume P = 1 for this method
        int cyclomaticComplexity = edgeCount - nodeCount + 2;
        method.setComplexity(cyclomaticComplexity);

        super.visitEnd();
    }
}
