package org.refactor.util;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.refactor.common.DatasetConst;
import org.refactor.model.JavaClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ASMUtils {
    private static final Logger logger = LoggerFactory.getLogger(ASMUtils.class);

    public static String methodToString(String name, String descriptor) {
        String arguments = Arrays.stream(Type.getMethodType(descriptor).getArgumentTypes())
                .map(Type::getClassName)
                .collect(Collectors.joining(", "));

        return name + "(" + arguments + ")";
    }

    public static boolean isGetterOrSetter(String methodName, String descriptor, Map<String, String> privateFields) {
        Type returnType = Type.getMethodType(descriptor).getReturnType();
        Type[] argumentTypes = Type.getMethodType(descriptor).getArgumentTypes();

        for (Map.Entry<String, String> field : privateFields.entrySet()) {
            String fieldName = field.getKey();
            Type fieldType = Type.getType(field.getValue());

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

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isAbstract(int access) {
        return (access & Opcodes.ACC_ABSTRACT) != 0;
    }

    public static boolean isEnum(int access) {
        return (access & Opcodes.ACC_ENUM) != 0;
    }

    public static boolean isInterface(int access) {
        return (access & Opcodes.ACC_INTERFACE) != 0;
    }

    public static boolean isInnerClass(String name) {
        return StringUtils.contains(name, "$");
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isConstructor(String name) {
        return StringUtils.equals(name, "<init>")
                || StringUtils.equals(name, "<clinit>");
    }

    public static boolean isFromJava(String name) {
        return (name.startsWith("java/") ||
                name.startsWith("javax/") ||
                name.startsWith("org/omg/") ||
                name.startsWith("org/w3c/dom") ||
                name.startsWith("org/xml/sax/"));
    }

    public static void loadMethodsToClass(JavaClass clazz) {
        try {
            Class<?> aClass = DatasetConst.urlCL.loadClass(Type.getObjectType(clazz.getName()).getClassName());
            for (Method m : aClass.getMethods()) {
                clazz.createMethod(m.getName(), Type.getMethodDescriptor(m));
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    public static boolean isOverride(String superName,
                                     String methodName,
                                     String descriptor) {
        try {
            Class<?> superClass = DatasetConst.urlCL.loadClass(Type.getObjectType(superName).getClassName());
            for (Method m : superClass.getMethods()) {
                if (ASMUtils.isMethodEqual(m, methodName, descriptor)) {
                    return true;
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            return false;
        }

        return false;
    }

    public static boolean isPrimitiveType(Type type) {
        return Arrays.asList(PRIMITIVE_TYPES).contains(type);
    }

    public static boolean isPrimitiveType(String type) {
        return Arrays.stream(PRIMITIVE_TYPES)
                .map(Type::getInternalName)
                .anyMatch(type::equals);
    }

    public static List<Type> getMethodSignatureType(String descriptor) {
        Type methodType = Type.getMethodType(descriptor);
        List<Type> types = new ArrayList<>();
        for (Type argType : methodType.getArgumentTypes()) {
            if (isArray(argType)) {
                types.add(argType.getElementType());
                continue;
            }
            types.add(argType);
        }
        Type returnType = methodType.getReturnType();
        types.add(isArray(returnType) ? returnType.getElementType() : returnType);

        return types;
    }

    public static Type getFieldType(String descriptor) {
        Type type = Type.getType(descriptor);
        return isArray(type) ? type.getElementType() : type;
    }

    private static boolean isArray(Type type) {
        return Type.ARRAY == type.getSort();
    }

    private static final Type[] PRIMITIVE_TYPES = new Type[]{
            Type.VOID_TYPE,
            Type.BOOLEAN_TYPE,
            Type.CHAR_TYPE,
            Type.BYTE_TYPE,
            Type.SHORT_TYPE,
            Type.INT_TYPE,
            Type.FLOAT_TYPE,
            Type.LONG_TYPE,
            Type.DOUBLE_TYPE,
    };

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
