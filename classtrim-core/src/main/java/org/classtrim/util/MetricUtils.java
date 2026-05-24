package org.classtrim.util;

import org.apache.commons.lang3.tuple.Triple;
import org.classtrim.common.ClassMetrics;
import org.classtrim.common.ThresholdViolationCounts;
import org.classtrim.common.Threshold;
import org.classtrim.model.JavaClass;
import org.classtrim.model.JavaMethod;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility class for computing OO metrics (WMC, CBO, RFC) on a per-class basis
 * and counting how many classes exceed configured thresholds.
 *
 * <p>All methods operate on a {@code Map<JavaClass, List<JavaMethod>>} that maps
 * each refactorable class to its declared methods. This map can be obtained from
 * {@link org.classtrim.model.JavaProject#toMap()} for the original project state,
 * or from the optimizer's solution encoding for a refactored state.</p>
 *
 * <h2>Metrics</h2>
 * <ul>
 *   <li><strong>WMC</strong> (Weighted Methods per Class) — method count in the class.</li>
 *   <li><strong>CBO</strong> (Coupling Between Objects) — count of distinct non-primitive,
 *       non-JDK classes coupled to this class (via fields, parameters, invocations).</li>
 *   <li><strong>RFC</strong> (Response For Class) — count of distinct methods reachable
 *       from this class (own methods + directly invoked methods).</li>
 * </ul>
 */
public class MetricUtils {

    private MetricUtils() {} // utility class

    // =========================================================================
    // Aggregate metrics
    // =========================================================================

    /**
     * Counts how many classes exceed each threshold and returns the result as a
     * {@link ThresholdViolationCounts} (one count per metric type).
     *
     * @param methodsByClass class → declared methods mapping
     * @param threshold      the WMC/CBO/RFC thresholds to compare against
     * @return counts of classes exceeding each threshold
     */
    public static ThresholdViolationCounts calculateMetric(Map<JavaClass, List<JavaMethod>> methodsByClass, Threshold threshold) {
        long wmcOverThreshold = countClassWmcOverThreshold(methodsByClass, threshold.getWMC());
        long cboOverThreshold = countClassCboOverThreshold(methodsByClass, threshold.getCBO());
        long rfcOverThreshold = countClassRfcOverThreshold(methodsByClass, threshold.getRFC());

        return ThresholdViolationCounts.of(wmcOverThreshold, cboOverThreshold, rfcOverThreshold);
    }

    // =========================================================================
    // Per-class metrics
    // =========================================================================

    /**
     * Computes WMC, CBO, and RFC for every class in the map and returns a
     * {@link ClassMetrics} record per class.
     *
     * @param methodsByClass class → declared methods mapping
     * @return per-class metrics keyed by class
     */
    public static Map<JavaClass, ClassMetrics> computePerClassMetrics(Map<JavaClass, List<JavaMethod>> methodsByClass) {
        return methodsByClass.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey,
                        entry -> new ClassMetrics(
                                entry.getValue().size(),
                                computeCbo(entry.getKey(), entry.getValue()),
                                computeRfc(entry.getValue()))));
    }

    /**
     * @deprecated Use {@link #computePerClassMetrics(Map)} which returns {@link ClassMetrics} instead of Triple.
     */
    @Deprecated
    public static Map<JavaClass, Triple<Integer, Integer, Integer>> getMetricsOfClass(Map<JavaClass, List<JavaMethod>> methodsByClass) {
        return methodsByClass.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey,
                        entry -> Triple.of(entry.getValue().size(),
                                computeCbo(entry.getKey(), entry.getValue()),
                                computeRfc(entry.getValue()))));
    }

    // =========================================================================
    // Count classes exceeding thresholds
    // =========================================================================

    /**
     * Counts classes whose WMC (method count) exceeds the given threshold.
     */
    public static long countClassWmcOverThreshold(Map<JavaClass, List<JavaMethod>> methodsByClass, int threshold) {
        return getWmcOfClass(methodsByClass).values().stream()
                .filter(wmc -> wmc > threshold)
                .count();
    }

    /**
     * Counts classes whose CBO (coupling) exceeds the given threshold.
     */
    public static long countClassCboOverThreshold(Map<JavaClass, List<JavaMethod>> methodsByClass, int threshold) {
        return getCboOfClass(methodsByClass).values().stream()
                .filter(cbo -> cbo > threshold)
                .count();
    }

    /**
     * Counts classes whose RFC (response set size) exceeds the given threshold.
     */
    public static long countClassRfcOverThreshold(Map<JavaClass, List<JavaMethod>> methodsByClass, int threshold) {
        return getRfcOfClass(methodsByClass).values().stream()
                .filter(rfc -> rfc > threshold)
                .count();
    }

    // =========================================================================
    // Sum of excess over thresholds (guiding objectives)
    // =========================================================================

    /**
     * Sums the amount by which each class's WMC exceeds the threshold.
     * Classes at or below the threshold contribute 0.
     */
    public static long sumClassWmcOverThreshold(Map<JavaClass, List<JavaMethod>> methodsByClass, int threshold) {
        return getWmcOfClass(methodsByClass).values().stream()
                .mapToInt(wmc -> Math.max(wmc - threshold, 0))
                .sum();
    }

    /**
     * Sums the amount by which each class's CBO exceeds the threshold.
     */
    public static long sumClassCboOverThreshold(Map<JavaClass, List<JavaMethod>> methodsByClass, int threshold) {
        return getCboOfClass(methodsByClass).values().stream()
                .mapToInt(cbo -> Math.max(cbo - threshold, 0))
                .sum();
    }

    /**
     * Sums the amount by which each class's RFC exceeds the threshold.
     */
    public static long sumClassRfcOverThreshold(Map<JavaClass, List<JavaMethod>> methodsByClass, int threshold) {
        return getRfcOfClass(methodsByClass).values().stream()
                .mapToInt(rfc -> Math.max(rfc - threshold, 0))
                .sum();
    }

    // =========================================================================
    // Per-class individual metric maps
    // =========================================================================

    /**
     * Returns WMC (method count) per class.
     */
    public static Map<JavaClass, Integer> getWmcOfClass(Map<JavaClass, List<JavaMethod>> methodsByClass) {
        return methodsByClass.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> entry.getValue().size()));
    }

    /**
     * Returns CBO (coupling between objects) per class.
     */
    public static Map<JavaClass, Integer> getCboOfClass(Map<JavaClass, List<JavaMethod>> methodsByClass) {
        return methodsByClass.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> computeCbo(e.getKey(), e.getValue())));
    }

    /**
     * Returns RFC (response for class) per class.
     */
    public static Map<JavaClass, Integer> getRfcOfClass(Map<JavaClass, List<JavaMethod>> methodsByClass) {
        return methodsByClass.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> computeRfc(e.getValue())));
    }

    // =========================================================================
    // Computation helpers (package-private for testing and MCP explain_suggestion)
    // =========================================================================

    /**
     * Computes CBO for a single class: the count of distinct non-primitive, non-JDK
     * classes that this class depends on through method invocations, field types,
     * parameter/return types, superclass, and interfaces.
     */
    static int computeCbo(JavaClass clazz, List<JavaMethod> methods) {
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

    /**
     * Computes RFC for a single class: the count of distinct methods reachable from
     * this class (own declared methods + all methods they directly invoke).
     */
    static int computeRfc(List<JavaMethod> methods) {
        return (int) Stream.concat(methods.stream(),
                        methods.stream().map(JavaMethod::getInvokeMethods).flatMap(List::stream))
                .distinct()
                .count();
    }

    /**
     * Returns the list of classes that a method is coupled to (through invocations
     * and parameter/return type references).
     */
    static List<JavaClass> getCouplingOfMethod(JavaMethod method) {
        List<JavaClass> couplings = method.getInvokeMethods().stream()
                .map(JavaMethod::getClazz)
                .collect(Collectors.toList());
        couplings.addAll(method.getCoupling());
        return couplings;
    }
}
