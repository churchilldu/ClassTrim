package org.refactor.util;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.Map;

public class ASMUtils {
    private static final Logger logger = LoggerFactory.getLogger(ASMUtils.class);

    public static boolean isGetterOrSetter(String methodName, String descriptor, Map<String, Type> privateFields) {
        Type returnType = Type.getMethodType(descriptor).getReturnType();
        Type[] argumentTypes = Type.getMethodType(descriptor).getArgumentTypes();

        for (Map.Entry<String, Type> field : privateFields.entrySet()) {
            String fieldName = field.getKey();
            Type fieldType = field.getValue();

            if (StringUtils.equalsIgnoreCase("get" + fieldName, methodName)) {
                if (ObjectUtils.isEmpty(argumentTypes) && fieldType.equals(returnType)) {
                    return true;
                }
            }

            if (StringUtils.equalsIgnoreCase("set" + fieldName, methodName)) {
                if (returnType.equals(Type.VOID_TYPE)) {
                    return true;
                }
            }

            if (methodName.startsWith("is")) {
                if (returnType.equals(Type.BOOLEAN_TYPE)) {
                    return true;
                }
            }

            if (methodName.startsWith("_")) {
                return true;
            }
        }

        return false;
    }

    public static boolean isPrivate(int access) {
        return (access & Opcodes.ACC_PRIVATE) != 0;
    }

    public static boolean isPublic(int access) {
        return (access & Opcodes.ACC_PUBLIC) != 0;
    }

    public static boolean isAbstract(int access) {
        return (access & Opcodes.ACC_ABSTRACT) != 0;
    }

    public static boolean isInnerClass(String name) {
        return StringUtils.contains(name, "$");
    }

    public static boolean isConstructor(String name) {
        return StringUtils.equals(name, "<init>");
    }

    public static boolean isFromJava(String name) {
        return (name.startsWith("java/") ||
                name.startsWith("javax/") ||
                name.startsWith("org/omg/") ||
                name.startsWith("org/w3c/dom.") ||
                name.startsWith("org/xml/sax/"));
    }

    public static boolean isOverride(URLClassLoader urlCL,
                                     String superName,
                                     String[] interfaces,
                                     String methodName,
                                     String descriptor) {
        try {
            Class<?> superClass = urlCL.loadClass(Type.getObjectType(superName).getClassName());
            for (Method m : superClass.getMethods()) {
                if (ASMUtils.isMethodEqual(m, methodName, descriptor)) {
                    return true;
                }
            }

            for (String name : interfaces) {
                Class<?> aInterface = urlCL.loadClass(Type.getObjectType(name).getClassName());
                for (Method m : aInterface.getMethods()) {
                    if (ASMUtils.isMethodEqual(m, methodName, descriptor)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        return false;
    }

    private static boolean isMethodEqual(Method m1, String m2Name, String m2Descriptor) {
        Class<?>[] m1Params = m1.getParameterTypes();
        Type[] m2Params = Type.getMethodType(m2Descriptor).getArgumentTypes();

        if (StringUtils.equals(m1.getName(), m2Name)
                && m1Params.length == m2Params.length) {
            for (int i = 0; i < m1Params.length; i++) {
                if (!StringUtils.equals(m1Params[i].getName(),
                        m2Params[i].getClassName())) {
                    return false;
                }
            }
            return true;
        }

        return false;
    }

}
