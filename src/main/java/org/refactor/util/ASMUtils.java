package org.refactor.util;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.refactor.model.JavaMethod;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.Map;

public class ASMUtils {
    public static boolean isGetterOrSetter(JavaMethod method, Map<String, Type> privateFields) {
        String methodName = method.getName();
        Type returnType = method.getReturnType();
        Type[] argumentTypes = method.getArgumentTypes();

        for (Map.Entry<String, Type> field : privateFields.entrySet()) {
            String name = field.getKey();
            Type type = field.getValue();

            if (StringUtils.equalsIgnoreCase("get" + name, methodName)) {
                if (ObjectUtils.isEmpty(argumentTypes) && type.equals(returnType)) {
                    return true;
                }
            }

            if (StringUtils.equalsIgnoreCase("set" + name, methodName)) {
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

    public static boolean isMethodEqual(Method m1, JavaMethod m2) {
        Class<?>[] parameterTypes = m1.getParameterTypes();
        Type[] argumentTypes = m2.getArgumentTypes();
        if (StringUtils.equals(m1.getName(), m2.getName())
                && parameterTypes.length == argumentTypes.length) {
            for (int i = 0; i < parameterTypes.length; i++) {
                if (!StringUtils.equals(parameterTypes[i].getName()
                        , argumentTypes[i].getClassName())) {
                    return false;
                }
            }
            return true;
        }

        return false;
    }
}
