package org.refactor.util;

import org.refactor.model.JavaClass;
import org.refactor.model.JavaMethod;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public class MetricUtils {
    // WMC Weighted Method per Class
    public static long countClassWmcOverThreshold(Map<JavaClass, List<JavaMethod>> methodsByClass, int threshold) {
        return MetricUtils.getWmcByClass(methodsByClass).values().stream().filter(
                wmc -> wmc > threshold
        ).count();
    }

    // CBO Coupling Between Objects
    public static long countClassCboOverThreshold(Map<JavaClass, List<JavaMethod>> methodsByClass, int threshold) {
        return MetricUtils.getCboByClass(methodsByClass).values().stream().filter(
                cbo -> cbo > threshold
        ).count();
    }

    // RFC
    public static long countClassRfcOverThreshold(Map<JavaClass, List<JavaMethod>> methodsByClass, int threshold) {
        return MetricUtils.getRfcByClass(methodsByClass).values().stream().filter(
                rfc -> rfc > threshold
        ).count();
    }

    public static long sumClassWmcOverThreshold(Map<JavaClass, List<JavaMethod>> methodsByClass, int threshold) {
        return MetricUtils.getWmcByClass(methodsByClass).values().stream().mapToInt(
                wmc -> Math.max(wmc - threshold, 0)
        ).sum();
    }

    public static long sumClassCboOverThreshold(Map<JavaClass, List<JavaMethod>> methodsByClass, int threshold) {
        return MetricUtils.getCboByClass(methodsByClass).values().stream().mapToInt(
                cbo -> Math.max(cbo - threshold, 0)
        ).sum();
    }

    public static long sumClassRfcOverThreshold(Map<JavaClass, List<JavaMethod>> methodsByClass, int threshold) {
        return MetricUtils.getRfcByClass(methodsByClass).values().stream().mapToInt(
                rfc -> Math.max(rfc - threshold, 0)
        ).sum();
    }

    public static Map<JavaClass, Integer> getWmcByClass(Map<JavaClass, List<JavaMethod>> methodsByClass) {
        return methodsByClass.entrySet().stream().collect(
                Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().size()));
    }

    public static Map<JavaClass, Integer> getCboByClass(Map<JavaClass, List<JavaMethod>> methodsByClass) {
        return methodsByClass.entrySet().stream().collect(
                Collectors.toMap(Map.Entry::getKey, e -> computeCbo(e.getKey(), e.getValue())));
    }

    private static int computeCbo(JavaClass clazz, List<JavaMethod> methods) {
        List<JavaClass> coupling = methods.stream()
                .map(MetricUtils::getCouplingOfMethod)
                .flatMap(List::stream)
                .collect(Collectors.toList());
        clazz.getSuperClass().ifPresent(coupling::add);
        coupling.addAll(clazz.getInterfaces());
        coupling.addAll(clazz.getFieldsType());

        return coupling.stream()
                .filter(Predicate.not(clazz::equals))
                .map(JavaClass::getName)
                .filter(Predicate.not(ASMUtils::isFromJava))
                .collect(Collectors.toSet())
                .size();
    }

    private static List<JavaClass> getCouplingOfMethod(JavaMethod method) {
        List<JavaClass> couplings = method.getInvokeMethods().stream()
                .map(JavaMethod::getClazz)
                .collect(Collectors.toList());
        couplings.addAll(method.getSignature());

        return couplings;
    }

    public static Map<JavaClass, Integer> getRfcByClass(Map<JavaClass, List<JavaMethod>> methodsByClass) {
        return methodsByClass.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> computeRfc(e.getValue())));
    }

    private static int computeRfc(List<JavaMethod> methods) {
        return methods.stream()
                .map(JavaMethod::getInvokeMethods)
                .flatMap(List::stream)
                .collect(Collectors.toSet())
                .size() + methods.size();
    }

}


