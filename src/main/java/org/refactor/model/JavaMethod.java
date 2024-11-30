package org.refactor.model;

import org.apache.commons.lang3.StringUtils;
import org.refactor.common.DataSetConst;
import org.refactor.util.ASMUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class JavaMethod extends JavaObject {
    private final Logger logger = LoggerFactory.getLogger(JavaMethod.class);

    private final JavaClass clazz;
    private final String descriptor;
    private int access;
    private boolean isGetterOrSetter;
    private int complexity = 0;

    public JavaMethod(JavaClass clazz, String name, String descriptor) {
        super(name);
        this.clazz = clazz;
        this.descriptor = descriptor;
    }

    public JavaClass getCls() {
        return clazz;
    }

    public int getComplexity() {
        return complexity;
    }

    public void setComplexity(int complexity) {
        this.complexity = complexity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof JavaMethod) {
            return StringUtils.equals(this.getName(), ((JavaMethod) o).getName())
                    && StringUtils.equals(this.descriptor, ((JavaMethod) o).getDescriptor())
                    && this.getCls().equals(((JavaMethod) o).getCls());
        }

        return false;
    }

    @Override
    public String toString() {
        return this.getName();
    }

    public boolean canRefactor() {
        return (access & Opcodes.ACC_PUBLIC) != 0
                && !isGetterOrSetter
                && !isOverride();
    }

    public void setAccess(int access) {
        this.access = access;
    }

    public boolean isOverride() {
        try {
            URLClassLoader urlCL = URLClassLoader.newInstance(DataSetConst.Ant.URL);
            Class<?> aClass = urlCL.loadClass(this.getCls().getQualifiedName());
            Class<?> superclass = aClass.getSuperclass();
            if (superclass != null) {
                for (Method m : superclass.getMethods()) {
                    if (ASMUtils.isMethodEqual(m, this)) {
                        return true;
                    }
                }
            }

            Class<?>[] interfaces = aClass.getInterfaces();
            for (Class<?> aInterface : interfaces) {
                for (Method m : aInterface.getMethods()) {
                    if (ASMUtils.isMethodEqual(m, this)) {
                        return true;
                    }
                }
            }
        } catch (Exception e){
            logger.error(this.getCls().getQualifiedName());
            e.printStackTrace();
        }

        return false;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public void setGetterOrSetter(boolean getterOrSetter) {
        isGetterOrSetter = getterOrSetter;
    }

    public Type getReturnType() {
        return Type.getMethodType(descriptor).getReturnType();
    }

    public Type[] getArgumentTypes() {
        return Type.getMethodType(descriptor).getArgumentTypes();
    }
}
