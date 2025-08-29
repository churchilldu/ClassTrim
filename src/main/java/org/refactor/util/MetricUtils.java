package org.refactor.util;

import org.apache.commons.lang3.tuple.Triple;
import org.refactor.common.Metric;
import org.refactor.common.Threshold;
import org.refactor.model.JavaClass;
import org.refactor.model.JavaMethod;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class MetricUtils {
        
    /**
     * Calculate the metric of the refactored code
     */
    public static Metric calculateMetric(Map<JavaClass, List<JavaMethod>> methodsByClass, Threshold threshold) {
        long wmcOverThreshold = MetricUtils.countClassWmcOverThreshold(methodsByClass, threshold.getWMC());
        long cboOverThreshold = MetricUtils.countClassCboOverThreshold(methodsByClass, threshold.getCBO());
        long rfcOverThreshold = MetricUtils.countClassRfcOverThreshold(methodsByClass, threshold.getRFC());
        
        return Metric.of(wmcOverThreshold, cboOverThreshold, rfcOverThreshold);
    }

    public static Map<JavaClass, Triple<Integer, Integer, Integer>> getMetricsOfClass(Map<JavaClass, List<JavaMethod>> methodsByClass) {
        return methodsByClass.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey,
                        entry -> Triple.of(entry.getValue().size(),
                                computeCbo(entry.getKey(), entry.getValue()),
                                computeRfc(entry.getValue()))));
    }

    // WMC Weighted Method per Class
    public static long countClassWmcOverThreshold(Map<JavaClass, List<JavaMethod>> methodsByClass, int threshold) {
        return MetricUtils.getWmcOfClass(methodsByClass).values().stream().filter(
                wmc -> wmc > threshold
        ).count();
    }

    // CBO Coupling Between Objects
    public static long countClassCboOverThreshold(Map<JavaClass, List<JavaMethod>> methodsByClass, int threshold) {
        return MetricUtils.getCboOfClass(methodsByClass).values().stream().filter(
                cbo -> cbo > threshold
        ).count();
    }

    // RFC
    public static long countClassRfcOverThreshold(Map<JavaClass, List<JavaMethod>> methodsByClass, int threshold) {
        return MetricUtils.getRfcOfClass(methodsByClass).values().stream().filter(
                rfc -> rfc > threshold
        ).count();
    }

    public static long sumClassWmcOverThreshold(Map<JavaClass, List<JavaMethod>> methodsByClass, int threshold) {
        return MetricUtils.getWmcOfClass(methodsByClass).values().stream().mapToInt(
                wmc -> Math.max(wmc - threshold, 0)
        ).sum();
    }

    public static long sumClassCboOverThreshold(Map<JavaClass, List<JavaMethod>> methodsByClass, int threshold) {
        return MetricUtils.getCboOfClass(methodsByClass).values().stream().mapToInt(
                cbo -> Math.max(cbo - threshold, 0)
        ).sum();
    }

    public static long sumClassRfcOverThreshold(Map<JavaClass, List<JavaMethod>> methodsByClass, int threshold) {
        return MetricUtils.getRfcOfClass(methodsByClass).values().stream().mapToInt(
                rfc -> Math.max(rfc - threshold, 0)
        ).sum();
    }

    public static Map<JavaClass, Integer> getWmcOfClass(Map<JavaClass, List<JavaMethod>> methodsByClass) {
        return methodsByClass.entrySet().stream().collect(
                Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().size()));
    }

    public static Map<JavaClass, Integer> getCboOfClass(Map<JavaClass, List<JavaMethod>> methodsByClass) {
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

        return (int) coupling.stream()
                .filter(Predicate.not(clazz::equals))
                .map(JavaClass::getName)
                .filter(Predicate.not(ASMUtils::isPrimitiveType))
                .filter(Predicate.not(ASMUtils::isFromJava))
                .distinct()
                .count();
    }

    protected static List<JavaClass> getCouplingOfMethod(JavaMethod method) {
        List<JavaClass> couplings = method.getInvokeMethods().stream()
                .map(JavaMethod::getClazz)
                .collect(Collectors.toList());
        couplings.addAll(method.getCoupling());

        return couplings;
    }

    public static Map<JavaClass, Integer> getRfcOfClass(Map<JavaClass, List<JavaMethod>> methodsByClass) {
        return methodsByClass.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> computeRfc(e.getValue())));
    }

    private static int computeRfc(List<JavaMethod> methods) {
        return (int) Stream.concat(methods.stream(),
                        methods.stream().map(JavaMethod::getInvokeMethods).flatMap(List::stream))
                .distinct()
                .count();
    }

}
