package org.classtrim.plugin;

import org.classtrim.core.metric.Threshold;
import org.classtrim.core.config.RefactoringConfig;
import org.classtrim.core.model.BinaryPathProjectSource;
import org.classtrim.core.model.ProjectSource;

import java.util.List;

/**
 * Pure builder that turns plugin settings plus discovered compiler-output roots into the
 * {@link ProjectSource} and {@link RefactoringConfig} consumed by
 * {@code org.classtrim.core.service.ClassTrimService}.
 *
 * <p>This class performs no transformation on any field: every value passed in is forwarded
 * to the corresponding {@code classtrim-core} constructor argument unchanged. Validation of
 * those values (population/iteration minimums, non-empty roots, missing defaults) is the
 * responsibility of {@code validate(...)} which lives in this same class but is implemented
 * separately.</p>
 *
 * <p>The class is a stateless utility: it cannot be instantiated and exposes only static
 * factory methods.</p>
 */
public final class AnalysisRunFactory {

    private AnalysisRunFactory() {
        // Utility class - no instances.
    }

    /**
     * Builds the {@link ProjectSource} for an Analysis_Run.
     *
     * <p>Returns {@code new BinaryPathProjectSource(projectName, roots, t)} with no
     * transformation of any field. {@code BinaryPathProjectSource} itself defensively
     * copies the {@code roots} list via {@link List#copyOf}.</p>
     *
     * @param projectName the IDE_Project name, forwarded as-is
     * @param roots       the resolved Compiler_Output_Roots, forwarded as-is
     * @param t           the metric threshold, forwarded as-is
     * @return a {@link BinaryPathProjectSource} carrying the supplied values
     */
    public static ProjectSource buildSource(String projectName, List<String> roots, Threshold t) {
        return new BinaryPathProjectSource(projectName, roots, t);
    }

    /**
     * Builds the {@link RefactoringConfig} for an Analysis_Run.
     *
     * <p>Returns {@code new RefactoringConfig(t, populationSize, maxIterations)} with no
     * transformation of any field.</p>
     *
     * @param t              the metric threshold, forwarded as-is
     * @param populationSize the NSGA-III population size, forwarded as-is
     * @param maxIterations  the NSGA-III maximum iteration count, forwarded as-is
     * @return a {@link RefactoringConfig} carrying the supplied values
     */
    public static RefactoringConfig buildConfig(Threshold t, int populationSize, int maxIterations) {
        return new RefactoringConfig(t, populationSize, maxIterations);
    }

    /**
     * Builds the {@link RefactoringConfig} for an Analysis_Run with explicit
     * control over guiding objectives.
     *
     * @param t                      the metric threshold, forwarded as-is
     * @param populationSize         the NSGA-III population size, forwarded as-is
     * @param maxIterations          the NSGA-III maximum iteration count, forwarded as-is
     * @param useGuidingObjectives   whether to include the sum-based guiding objectives (4–6)
     * @return a {@link RefactoringConfig} carrying the supplied values
     */
    public static RefactoringConfig buildConfig(Threshold t, int populationSize, int maxIterations,
                                                boolean useGuidingObjectives) {
        return new RefactoringConfig(t, populationSize, maxIterations, useGuidingObjectives);
    }

    /**
     * Builds a {@link Threshold} directly from a {@link SettingsView} snapshot. The view's
     * {@code wmc}, {@code cbo}, and {@code rfc} fields are forwarded to the
     * {@link Threshold} constructor in that order with no transformation.
     *
     * @param v the settings snapshot
     * @return a {@link Threshold} carrying {@code (v.wmc(), v.cbo(), v.rfc())}
     */
    public static Threshold threshold(SettingsView v) {
        return new Threshold(v.wmc(), v.cbo(), v.rfc());
    }

    /**
     * Builds a {@link Threshold} directly from primitive metric values. The arguments are
     * forwarded to the {@link Threshold} constructor in WMC, CBO, RFC order with no
     * transformation.
     *
     * @param wmc the Weighted Methods per Class threshold
     * @param cbo the Coupling Between Objects threshold
     * @param rfc the Response For a Class threshold
     * @return a {@link Threshold} carrying the supplied values
     */
    public static Threshold threshold(int wmc, int cbo, int rfc) {
        return new Threshold(wmc, cbo, rfc);
    }

    /**
     * Validates the inputs of an Analysis_Run without mutating any persisted state.
     *
     * <p>Validation order is deterministic so that error notifications are predictable
     * even when several violations are simultaneously present. Per the design's
     * Property 3, precedence is {@code NoCompilerRoots < MinValueViolation} (i.e.
     * {@code NoCompilerRoots} is reported first), and within {@code MinValueViolation}
     * {@code populationSize} is checked before {@code maxIterations}:</p>
     *
     * <ol>
     *   <li>{@code roots == null || roots.isEmpty()} &rarr;
     *       {@code Failure(NoCompilerRoots())} (R3.7).</li>
     *   <li>{@code v.populationSize() < 1} &rarr;
     *       {@code Failure(MinValueViolation("populationSize", v.populationSize()))} (R3.5).</li>
     *   <li>{@code v.maxIterations() < 1} &rarr;
     *       {@code Failure(MinValueViolation("maxIterations", v.maxIterations()))} (R3.5).</li>
     *   <li>Otherwise, build the threshold, source, and config via
     *       {@link #threshold(SettingsView)}, {@link #buildSource(String, List, Threshold)},
     *       and {@link #buildConfig(Threshold, int, int)} and return
     *       {@code Success(new RunInputs(source, config))}.</li>
     * </ol>
     *
     * <p>On any failure branch, no {@link ProjectSource} and no {@link RefactoringConfig}
     * is constructed: the builder calls live exclusively on the success branch below the
     * {@code if} chain. {@link SettingsView} is read-only by construction (it is an
     * immutable snapshot), so this method cannot mutate the persisted
     * {@code ClassTrimSettingsState.State} — in particular, it never calls
     * {@code ClassTrimSettingsState.updateFrom(...)}.</p>
     *
     * <p><b>Note on {@code MissingDefault}:</b> the
     * {@link ValidationError.MissingDefault} variant is currently unreachable from this
     * method because {@code ClassTrimSettingsState.defaults()} always returns a
     * non-{@code null} {@code Defaults} instance with every field populated. The variant
     * is kept on the {@link ValidationError} hierarchy so that future settings sources
     * (for example, an external profile loader) can surface a missing-default failure
     * without an API break, and {@code validate} can grow a check for it then. R3.6 is
     * otherwise satisfied by construction today.</p>
     *
     * @param v           the immutable settings snapshot
     * @param roots       the resolved Compiler_Output_Roots; may be {@code null} or empty
     * @param projectName the IDE_Project name, forwarded to {@link #buildSource} on success
     * @return {@code Success(RunInputs)} when all inputs are valid, otherwise a
     *         {@code Failure} carrying the offending {@link ValidationError}
     */
    public static Result<RunInputs, ValidationError> validate(
            SettingsView v, List<String> roots, String projectName) {
        return validate(v, roots, projectName, true);
    }

    public static Result<RunInputs, ValidationError> validate(
            SettingsView v, List<String> roots, String projectName, boolean useGuidingObjectives) {
        if (roots == null || roots.isEmpty()) {
            return Result.failure(new ValidationError.NoCompilerRoots());
        } else if (v.populationSize() < 1) {
            return Result.failure(
                    new ValidationError.MinValueViolation("populationSize", v.populationSize()));
        } else if (v.maxIterations() < 1) {
            return Result.failure(
                    new ValidationError.MinValueViolation("maxIterations", v.maxIterations()));
        } else {
            // Success branch: only here do we construct a ProjectSource / RefactoringConfig.
            // MissingDefault is currently unreachable (see Javadoc above) and is therefore
            // not checked here; the ValidationError.MissingDefault variant remains in the
            // sealed hierarchy for future expansion.
            Threshold t = threshold(v);
            ProjectSource source = buildSource(projectName, roots, t);
            RefactoringConfig config = buildConfig(t, v.populationSize(), v.maxIterations(), useGuidingObjectives);
            return Result.success(new RunInputs(source, config));
        }
    }
}
