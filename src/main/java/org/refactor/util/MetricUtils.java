package org.refactor.util;

import org.refactor.model.JavaClass;
import org.refactor.model.JavaMethod;

import java.util.*;
import java.util.stream.Collectors;


public class MetricUtils {

    // WMC Weighted Method per Class
    public static long countClassWmcOverThreshold(Map<JavaClass, List<JavaMethod>> methodsByClass, int threshold) {
        Map<JavaClass, Integer> wmcByClass = new HashMap<>();
        methodsByClass.forEach(
                (clazz, methods) ->
                        wmcByClass.put(clazz, methods.parallelStream().mapToInt(JavaMethod::getComplexity).sum())
        );

        return wmcByClass.values().parallelStream().filter(
                wmc -> wmc > threshold
        ).count();
    }

    // CBO Coupling Between Objects
    public static long countClassCboOverThreshold(Map<JavaClass, List<JavaMethod>> methodsByClass, int threshold) {
        Map<JavaClass, Integer> cboByClass = new HashMap<>();

        methodsByClass.forEach(
                (clazz, methods) -> {
                    Set<String> externalClass = new HashSet<>();
                    Set<JavaClass> innerClass = new HashSet<>();
                    methods.forEach(
                            m -> {
                                externalClass.addAll(m.getFixedClasses());
                                innerClass.addAll(
                                        m.getInvokeMethods().parallelStream().map(JavaMethod::getClazz).collect(Collectors.toSet())
                                );
                            }
                    );
                    int cbo = externalClass.size() + innerClass.size();
                    if (innerClass.contains(clazz)) {
                        cbo--;
                    }

                    cboByClass.put(clazz, cbo);
                }
        );

        return cboByClass.values().stream().filter(
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

    private static Map<JavaClass, Integer> getWmcByClass(Map<JavaClass, List<JavaMethod>> methodsByClass) {
        Map<JavaClass, Integer> wmcByClass = new HashMap<>();
        methodsByClass.forEach(
                (clazz, methods) ->
                        wmcByClass.put(clazz, methods.parallelStream().mapToInt(JavaMethod::getComplexity).sum())
        );

        return wmcByClass;
    }

    private static Map<JavaClass, Integer> getCboByClass(Map<JavaClass, List<JavaMethod>> methodsByClass) {
        Map<JavaClass, Integer> cboByClass = new HashMap<>();

        methodsByClass.forEach(
                (clazz, methods) -> {
                    Set<String> fixedClass = new HashSet<>();
                    Set<JavaClass> classes = new HashSet<>();
                    methods.forEach(
                            m -> {
                                fixedClass.addAll(m.getFixedClasses());
                                classes.addAll(
                                        m.getInvokeMethods().parallelStream().map(JavaMethod::getClazz).collect(Collectors.toSet())
                                );
                                classes.addAll(m.getDependencies());
                            }
                    );
                    int cbo = fixedClass.size() + classes.size();
                    if (classes.contains(clazz)) {
                        cbo--;
                    }

                    cboByClass.put(clazz, cbo);
                }
        );

        return cboByClass;
    }

    private static Map<JavaClass, Integer> getRfcByClass(Map<JavaClass, List<JavaMethod>> methodsByClass) {
        Map<JavaClass, Integer> rfcByClass = new HashMap<>();
        methodsByClass.forEach(
                (clazz, methods) -> {
                    methods.forEach(
                            m -> rfcByClass.merge(clazz,
                                    (int) (m.getFixedMethods().size()
                                            + m.getInvokeMethods().stream()
                                            .filter(i -> !i.getClazz().equals(clazz))
                                            .count()),
                                    Integer::sum
                            )
                    );
                }
        );

        return rfcByClass;
    }

}


