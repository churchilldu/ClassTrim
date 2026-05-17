package org.classtrim.plugin;

/**
 * Sealed hierarchy of validation failures returned by {@code AnalysisRunFactory.validate(...)}.
 *
 * <p>Each variant carries the information the coordinator needs to emit a deterministic
 * notification naming the offending field while leaving the persisted
 * {@code ClassTrimSettingsState.State} untouched.</p>
 *
 * <ul>
 *   <li>{@link MinValueViolation} — population size or max iterations is less than 1
 *       (Requirements 3.5).</li>
 *   <li>{@link MissingDefault} — a settings field has no developer-assigned value and
 *       no declared default (Requirements 3.6).</li>
 *   <li>{@link NoCompilerRoots} — the resolved Compiler_Output_Roots list is empty
 *       (Requirements 3.7).</li>
 * </ul>
 */
public sealed interface ValidationError
        permits ValidationError.MinValueViolation,
                ValidationError.MissingDefault,
                ValidationError.NoCompilerRoots {

    /**
     * The persisted value of {@code fieldName} is less than the minimum allowed (1).
     *
     * @param fieldName logical settings field name (e.g. {@code "populationSize"} or
     *                  {@code "maxIterations"})
     * @param actual    the offending value as read from the {@link SettingsView}
     */
    record MinValueViolation(String fieldName, int actual) implements ValidationError {
    }

    /**
     * A {@code SettingsView} field has no developer-assigned value and no default
     * declared by {@code ClassTrimSettingsState.Defaults}.
     *
     * @param fieldName logical settings field name that lacks a default
     */
    record MissingDefault(String fieldName) implements ValidationError {
    }

    /**
     * The Compiler_Output_Roots list is empty, so no {@code ProjectSource} can be
     * constructed for the Analysis_Run.
     */
    record NoCompilerRoots() implements ValidationError {
    }
}
