package org.classtrim.core.engine;

import java.util.function.Supplier;

/**
 * Available multi-objective optimization algorithms for move-method refactoring.
 * Each enum constant carries a {@link Supplier} that produces a fresh
 * {@link RefactoringEngine} instance on demand.
 */
public enum AlgorithmType {
    NSGA_III("NSGA-III", NSGAIIIRefactoringEngine::new),
    NSGA_II("NSGA-II", NSGAIIRefactoringEngine::new);

    private final String displayName;
    private final Supplier<RefactoringEngine> engineSupplier;

    AlgorithmType(String displayName, Supplier<RefactoringEngine> engineSupplier) {
        this.displayName = displayName;
        this.engineSupplier = engineSupplier;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Creates a new {@link RefactoringEngine} instance for this algorithm type.
     */
    public RefactoringEngine createEngine() {
        return engineSupplier.get();
    }

    /**
     * Resolves an {@link AlgorithmType} from its display name (case-insensitive).
     * Returns {@link #NSGA_III} if the input doesn't match any known algorithm.
     */
    public static AlgorithmType fromDisplayName(String name) {
        for (AlgorithmType t : values()) {
            if (t.displayName.equalsIgnoreCase(name)) {
                return t;
            }
        }
        return NSGA_III;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
