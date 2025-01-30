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
        return MetricUtils.getWmcByClass(methodsByClass).values().parallelStream().filter(
                wmc -> wmc > threshold
        ).count();
    }

    // CBO Coupling Between Objects
    public static long countClassCboOverThreshold(Map<JavaClass, List<JavaMethod>> methodsByClass, int threshold) {
        return MetricUtils.getCboByClass(methodsByClass).values().parallelStream().filter(
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
        return MetricUtils.getWmcByClass(methodsByClass).values().parallelStream().mapToInt(
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
        return methodsByClass.entrySet().parallelStream().collect(
                Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().size()
                )
        );
    }

    public static Map<JavaClass, Integer> getCboByClass(Map<JavaClass, List<JavaMethod>> methodsByClass) {
        return methodsByClass.entrySet().parallelStream().collect(
                Collectors.toMap(
                        Map.Entry::getKey,
                        e -> computeCbo(e.getKey(), e.getValue())
                )
        );
    }

    private static int computeCbo(JavaClass clazz, List<JavaMethod> methods) {
        Set<JavaClass> couplings = methods.parallelStream()
                .map(m -> getCouplingsOfMethod(clazz, m))
                .flatMap(Set::parallelStream)
                .collect(Collectors.toSet());
        clazz.getSuperClass().ifPresent(couplings::add);
        couplings.addAll(clazz.getInterfaces());
        couplings.addAll(clazz.getFieldsType());

        return couplings.size();
    }

    private static Set<JavaClass> getCouplingsOfMethod(JavaClass clazz, JavaMethod method) {
        Set<JavaClass> couplings = method.getInvokeMethods().parallelStream()
                .map(JavaMethod::getClazz)
                .filter(c -> !ASMUtils.isFromJava(c.getName()))
                .filter(Predicate.not(clazz::equals))
                .filter(Predicate.not(clazz::isInherited))
                .collect(Collectors.toSet());
        couplings.addAll(method.getSignatureType());

        return couplings;
    }

    public static Map<JavaClass, Integer> getRfcByClass(Map<JavaClass, List<JavaMethod>> methodsByClass) {
        return methodsByClass.entrySet().parallelStream().collect(
                Collectors.toMap(
                        Map.Entry::getKey,
                        e -> computeRfc(e.getValue())
                )
        );
    }

    private static int computeRfc(List<JavaMethod> methods) {
        return methods.stream()
                .map(JavaMethod::getInvokeMethods)
                .flatMap(Set::parallelStream)
                .collect(Collectors.toSet())
                .size() + methods.size();
    }

}


