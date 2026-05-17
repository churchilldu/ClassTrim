package org.classtrim.core.config;

import lombok.Getter;
import org.classtrim.common.Threshold;

@Getter
public class RefactoringConfig {
    private final Threshold threshold;
    private final int populationSize;
    private final int maxIterations;
    private final boolean useGuidingObjectives;

    public RefactoringConfig(Threshold threshold, int populationSize, int maxIterations) {
        this(threshold, populationSize, maxIterations, true);
    }

    public RefactoringConfig(Threshold threshold, int populationSize, int maxIterations, boolean useGuidingObjectives) {
        this.threshold = threshold;
        this.populationSize = populationSize;
        this.maxIterations = maxIterations;
        this.useGuidingObjectives = useGuidingObjectives;
    }
}
