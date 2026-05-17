package org.classtrim.plugin;

import org.classtrim.core.config.RefactoringConfig;
import org.classtrim.core.model.ProjectSource;

/**
 * Bundle of the two arguments passed into {@code ClassTrimService.analyze}: the
 * {@link ProjectSource} describing the project name, compiled class roots, and
 * thresholds, and the {@link RefactoringConfig} carrying thresholds, population size,
 * and maximum iteration count.
 *
 * <p>This record is the success payload of {@code AnalysisRunFactory.validate(...)}.
 * It exists purely to make that validation function pure and easily testable.</p>
 */
public record RunInputs(ProjectSource source, RefactoringConfig config) {
}
