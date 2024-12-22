package org.refactor.visitor;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.refactor.model.JavaClass;
import org.refactor.model.JavaMethod;
import org.refactor.model.JavaProject;
import org.refactor.util.ASMUtils;

public class MethodVisitor extends org.objectweb.asm.MethodVisitor {
    private final JavaProject project;
    private final JavaMethod method;

    protected MethodVisitor(JavaProject project, JavaMethod method) {
        super(Opcodes.ASM9);
        this.project = project;
        this.method = method;
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
//        method.incComplexity();
//        // Do cbo count the class outside self project?
//        if (project.contain(owner)) {
//            if (ASMUtils.isInnerClass(owner)) {
//                method.addFixedClass(owner);
//                method.addFixedMethod(owner + name + descriptor);
//            } else {
//                JavaClass classOnCall = project.getOrCreateClass(owner);
//                JavaMethod invokeMethod = project.getOrCreateMethod(classOnCall, name, descriptor);
//                method.addInvokeMethod(invokeMethod);
//            }
//        } else if (!ASMUtils.isFromJava(owner)) {
//            method.addFixedClass(owner);
//            method.addFixedMethod(owner + name + descriptor);
//        }
    }

}
