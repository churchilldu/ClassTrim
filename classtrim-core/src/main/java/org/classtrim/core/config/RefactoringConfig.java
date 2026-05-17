package org.classtrim.core.config;

import lombok.Getter;
import org.classtrim.common.Threshold;

@Getter
public class RefactoringConfig {
    private final Threshold threshold;
    private final int populationSize;
    private final int maxIterations;

    public RefactoringConfig(Threshold threshold, int populationSize, int maxIterations) {
        this.threshold = threshold;
        this.populationSize = populationSize;
        this.maxIterations = maxIterations;
    }
}
