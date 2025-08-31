package org.refactor.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/*
 * The number of classes that exceed the threshold.
 */

@Getter
@AllArgsConstructor
public class Metric {
    private final long wmcOverThreshold; // weightedMethodComplexityOverThreshold    
    private final long cboOverThreshold; // couplingBetweenObjectsOverThreshold
    private final long rfcOverThreshold; // responseForClassOverThreshold

    public static final Metric ZERO = new Metric(0, 0, 0);

    public static Metric of(long wmcOverThreshold, long cboOverThreshold, long rfcOverThreshold) {
        if (wmcOverThreshold < 0) {
            throw new IllegalArgumentException("WMC over threshold count cannot be negative");
        }
        if (cboOverThreshold < 0) {
            throw new IllegalArgumentException("CBO over threshold count cannot be negative");
        }
        if (rfcOverThreshold < 0) {
            throw new IllegalArgumentException("RFC over threshold count cannot be negative");
        }
        return new Metric(wmcOverThreshold, cboOverThreshold, rfcOverThreshold);
    }
}
