package org.refactor.util;

import org.refactor.model.JavaClass;
import org.refactor.model.JavaMethod;
import org.refactor.model.JavaProject;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public class MetricUtils {

    // WMC Weighted Method per Class
    public static long countClassWmcOverThreshold(JavaProject project, List<Integer> solution) {
        return MetricUtils.countClassWmcOverThreshold(
                convertToMap(project, solution),
                project.getThreshold().getWMC()
        );
    }

    public static long countClassCboOverThreshold(JavaProject project, List<Integer> solution) {
        return MetricUtils.countClassCboOverThreshold(
                convertToMap(project, solution),
                project.getThreshold().getCBO()
        );
    }

    public static long countClassRfcOverThreshold(JavaProject project, List<Integer> solution) {
        return MetricUtils.countClassRfcOverThreshold(
                convertToMap(project, solution),
                project.getThreshold().getRFC()
        );
    }

    public static long countClassWmcOverThreshold(Map<JavaClass, List<JavaMethod>> methodsByClass, int threshold) {
        Map<JavaClass, Integer> wmcByClass = new HashMap<>();
        methodsByClass.forEach(
                (clazz, methods) ->
                        wmcByClass.put(clazz,
                                methods.parallelStream().mapToInt(JavaMethod::getComplexity).sum())

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
                                externalClass.addAll(m.getExternalClasses());
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
        Map<JavaClass, Integer> rfcByClass = new HashMap<>();
        methodsByClass.forEach(
                (clazz, methods) -> {
                    methods.forEach(
                            m -> rfcByClass.merge(clazz,
                                    (int) (m.getExternalMethods().size()
                                            + m.getInvokeMethods().stream()
                                            .filter(Predicate.not(i -> i.getClazz().equals(clazz)))
                                            .count()),
                                    Integer::sum
                            )
                    );
                }
        );

        return rfcByClass.values().stream().filter(
                rfc -> rfc > threshold
        ).count();
    }

    private static Map<JavaClass, List<JavaMethod>> convertToMap(JavaProject project, List<Integer> solution) {
        Map<JavaClass, List<JavaMethod>> methodsByClass = new HashMap<>();

        List<JavaMethod> methodList = project.getMethodList();
        List<JavaClass> classList = project.getClassList();
        for (int methodId = 0; methodId < solution.size(); methodId++) {
            Integer classId = solution.get(methodId);
            methodsByClass.computeIfAbsent(classList.get(classId), k -> new ArrayList<>())
                    .add(methodList.get(methodId));
        }

        return methodsByClass;
    }

}


