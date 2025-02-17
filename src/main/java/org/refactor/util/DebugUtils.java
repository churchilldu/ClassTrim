package org.refactor.util;

import org.refactor.model.JavaClass;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DebugUtils {
    public static void debugCbo(JavaClass clazz) {
            List<JavaClass> coupling = clazz.getDeclaredMethods().stream()
                    .map(MetricUtils::getCouplingOfMethod)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
            clazz.getSuperClass().ifPresent(coupling::add);
            coupling.addAll(clazz.getInterfaces());
            coupling.addAll(clazz.getFieldsType());

            coupling.stream()
                    .filter(Predicate.not(clazz::equals))
                    .map(JavaClass::getName)
                    .filter(Predicate.not(ASMUtils::isPrimitiveType))
                    .filter(Predicate.not(ASMUtils::isFromJava))
                    .distinct()
                    .map(s -> s.replace("/", "."))
                    .forEach(System.out::println);
    }
}
