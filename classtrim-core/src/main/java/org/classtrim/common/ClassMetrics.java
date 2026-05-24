package org.classtrim.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Immutable snapshot of the three OO metrics for a single class.
 *
 * <ul>
 *   <li><strong>WMC</strong> (Weighted Methods per Class) — the number of methods
 *       declared in the class. Higher values indicate a class that does too much.</li>
 *   <li><strong>CBO</strong> (Coupling Between Objects) — the number of distinct
 *       non-primitive, non-JDK classes that the class depends on (through fields,
 *       method parameters, return types, and invocations). Higher values indicate
 *       tight coupling to other classes.</li>
 *   <li><strong>RFC</strong> (Response For Class) — the number of distinct methods
 *       that can be invoked in response to a message to the class (its own methods
 *       plus all methods it directly calls). Higher values indicate a class that is
 *       hard to test and understand.</li>
 * </ul>
 *
 * <p>Use {@link #exceedsAny(Threshold)} to check whether this class violates
 * the configured thresholds.</p>
 */
@Getter
@AllArgsConstructor
public class ClassMetrics {
    private final int wmc;
    private final int cbo;
    private final int rfc;

    /**
     * Returns {@code true} if any of the three metrics exceeds its corresponding
     * threshold value.
     */
    public boolean exceedsAny(Threshold threshold) {
        return wmc > threshold.getWMC()
                || cbo > threshold.getCBO()
                || rfc > threshold.getRFC();
    }

    /**
     * Returns {@code true} if WMC exceeds the threshold.
     */
    public boolean exceedsWmc(Threshold threshold) {
        return wmc > threshold.getWMC();
    }

    /**
     * Returns {@code true} if CBO exceeds the threshold.
     */
    public boolean exceedsCbo(Threshold threshold) {
        return cbo > threshold.getCBO();
    }

    /**
     * Returns {@code true} if RFC exceeds the threshold.
     */
    public boolean exceedsRfc(Threshold threshold) {
        return rfc > threshold.getRFC();
    }
}
