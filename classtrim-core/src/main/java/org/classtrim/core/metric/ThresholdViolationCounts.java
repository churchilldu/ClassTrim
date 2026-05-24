package org.classtrim.core.metric;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The number of classes in a project (or refactored solution) that exceed
 * each configured metric threshold. This is what the NSGA-III/II optimizer
 * minimizes — fewer violations means better code quality.
 */
@Getter
@AllArgsConstructor
public class ThresholdViolationCounts {
    private final long classesExceedingWmc;
    private final long classesExceedingCbo;
    private final long classesExceedingRfc;

    public static final ThresholdViolationCounts ZERO = new ThresholdViolationCounts(0, 0, 0);

    public static ThresholdViolationCounts of(long classesExceedingWmc, long classesExceedingCbo, long classesExceedingRfc) {
        if (classesExceedingWmc < 0) {
            throw new IllegalArgumentException("WMC over threshold count cannot be negative");
        }
        if (classesExceedingCbo < 0) {
            throw new IllegalArgumentException("CBO over threshold count cannot be negative");
        }
        if (classesExceedingRfc < 0) {
            throw new IllegalArgumentException("RFC over threshold count cannot be negative");
        }
        return new ThresholdViolationCounts(classesExceedingWmc, classesExceedingCbo, classesExceedingRfc);
    }

    /** @deprecated Use {@link #getClassesExceedingWmc()} */
    @Deprecated
    public long getWmcOverThreshold() { return classesExceedingWmc; }

    /** @deprecated Use {@link #getClassesExceedingCbo()} */
    @Deprecated
    public long getCboOverThreshold() { return classesExceedingCbo; }

    /** @deprecated Use {@link #getClassesExceedingRfc()} */
    @Deprecated
    public long getRfcOverThreshold() { return classesExceedingRfc; }
}
