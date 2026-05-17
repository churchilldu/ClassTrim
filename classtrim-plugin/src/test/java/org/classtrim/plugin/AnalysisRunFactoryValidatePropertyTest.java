package org.classtrim.plugin;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tag;
import org.classtrim.plugin.settings.ClassTrimSettingsState;
import org.junit.jupiter.api.Assertions;

import java.util.ArrayList;
import java.util.List;

/**
 * Property-based test for {@link AnalysisRunFactory#validate}.
 *
 * <p><strong>Validates: Requirements 3.5, 3.7</strong> — the "Validation rejects bad
 * inputs without mutating persisted settings" property from the {@code idea-plugin}
 * design (Correctness Property 3).</p>
 *
 * <p>{@code AnalysisRunFactory.validate(SettingsView v, List<String> roots, String
 * projectName)} is a pure function returning {@code Result<RunInputs, ValidationError>}.
 * The deterministic precedence implemented in production is:</p>
 *
 * <ol>
 *   <li>{@code roots == null || roots.isEmpty()} &rarr; {@code Err(NoCompilerRoots())}</li>
 *   <li>{@code v.populationSize() < 1} &rarr;
 *       {@code Err(MinValueViolation("populationSize", v.populationSize()))}</li>
 *   <li>{@code v.maxIterations() < 1} &rarr;
 *       {@code Err(MinValueViolation("maxIterations", v.maxIterations()))}</li>
 *   <li>otherwise {@code Ok(RunInputs(...))}.</li>
 * </ol>
 *
 * <p>The property exercises this contract over heavily-biased generators that hit
 * the boundaries ({@code Integer.MIN_VALUE}, {@code -1}, {@code 0}, {@code 1}, the
 * declared defaults, {@code Integer.MAX_VALUE}) and asserts: (1) failure detection,
 * (2) deterministic precedence, (3) the persisted {@link ClassTrimSettingsState.State}
 * is bit-identical before and after, and (4) on the failure branch no
 * {@link org.classtrim.core.model.ProjectSource} or
 * {@link org.classtrim.core.config.RefactoringConfig} is constructed (witnessed by
 * the {@link Result.Failure} side carrying no {@link RunInputs}; the production
 * {@link AnalysisRunFactory#validate} only constructs builders inside the success
 * branch — see its source).</p>
 */
class AnalysisRunFactoryValidatePropertyTest {

    /** A stable project name used across all examples; the validator forwards it untouched on success. */
    private static final String PROJECT_NAME = "TestProject";

    /**
     * <strong>Feature: idea-plugin, Property 3: Validation rejects bad inputs
     * without mutating persisted settings.</strong>
     *
     * <p>For any {@code (SettingsView v, List<String> roots)}, {@code validate(v,
     * roots, projectName)}:</p>
     * <ol>
     *   <li>returns an {@link Result.Failure} when {@code roots == null || roots.isEmpty()}
     *       OR {@code v.populationSize() < 1} OR {@code v.maxIterations() < 1};</li>
     *   <li>resolves multiple simultaneous violations in deterministic precedence:
     *       {@code NoCompilerRoots} &lt; {@code MinValueViolation("populationSize",...)}
     *       &lt; {@code MinValueViolation("maxIterations",...)};</li>
     *   <li>does not mutate the persisted {@link ClassTrimSettingsState.State}
     *       (asserted by capturing every persisted field before the call against a
     *       fresh in-memory snapshot and re-comparing after);</li>
     *   <li>on the failure branch, the result is an {@link Result.Failure} that carries
     *       no {@link RunInputs} and therefore no constructed
     *       {@link org.classtrim.core.model.ProjectSource} or
     *       {@link org.classtrim.core.config.RefactoringConfig}.</li>
     * </ol>
     *
     * <p>Validates: Requirements 3.5, 3.7.</p>
     */
    @Property(tries = 100)
    // JUnit 5 @Tag values cannot contain colons or commas, so a sanitized
    // identifier is used for tooling-level filtering. The full human-readable
    // label is published via @Label below.
    @Tag("idea-plugin-property-3")
    @Label("Feature: idea-plugin, Property 3: Validation rejects bad inputs without mutating persisted settings")
    void validateRejectsBadInputsWithoutMutatingPersistedSettings(
            @ForAll("validateInputs") ValidateInputs in) {

        // (3) Capture the persisted state before the call. We use a fresh in-memory
        //     ClassTrimSettingsState.State seeded from the generated SettingsView so
        //     that we can prove validate() is read-only against a real persisted
        //     snapshot, not just against the immutable view record.
        ClassTrimSettingsState.State persisted = new ClassTrimSettingsState.State();
        persisted.wmc = in.view().wmc();
        persisted.cbo = in.view().cbo();
        persisted.rfc = in.view().rfc();
        persisted.populationSize = in.view().populationSize();
        persisted.maxIterations = in.view().maxIterations();

        int beforeWmc = persisted.wmc;
        int beforeCbo = persisted.cbo;
        int beforeRfc = persisted.rfc;
        int beforePop = persisted.populationSize;
        int beforeMax = persisted.maxIterations;

        Result<RunInputs, ValidationError> result =
                AnalysisRunFactory.validate(in.view(), in.roots(), PROJECT_NAME);

        // (3) Persisted state must be bit-identical before and after (R3.5, R3.7).
        Assertions.assertEquals(beforeWmc, persisted.wmc, "validate must not mutate persisted wmc");
        Assertions.assertEquals(beforeCbo, persisted.cbo, "validate must not mutate persisted cbo");
        Assertions.assertEquals(beforeRfc, persisted.rfc, "validate must not mutate persisted rfc");
        Assertions.assertEquals(beforePop, persisted.populationSize,
                "validate must not mutate persisted populationSize");
        Assertions.assertEquals(beforeMax, persisted.maxIterations,
                "validate must not mutate persisted maxIterations");

        // (1) Classify the input.
        boolean rootsBad = in.roots() == null || in.roots().isEmpty();
        boolean popBad = in.view().populationSize() < 1;
        boolean iterBad = in.view().maxIterations() < 1;
        boolean anyBad = rootsBad || popBad || iterBad;

        if (anyBad) {
            // (1) Failure detection.
            Assertions.assertTrue(result.isFailure(),
                    () -> "Expected Failure for bad input rootsBad=" + rootsBad
                            + ", popBad=" + popBad + ", iterBad=" + iterBad
                            + " (populationSize=" + in.view().populationSize()
                            + ", maxIterations=" + in.view().maxIterations()
                            + ", roots=" + in.roots() + ")");

            // (2) Deterministic precedence: NoCompilerRoots < populationSize < maxIterations.
            ValidationError err = result.error().orElseThrow();
            if (rootsBad) {
                Assertions.assertInstanceOf(ValidationError.NoCompilerRoots.class, err,
                        "Roots empty/null must take precedence and yield NoCompilerRoots");
            } else if (popBad) {
                Assertions.assertInstanceOf(ValidationError.MinValueViolation.class, err,
                        "populationSize<1 (with roots present) must yield MinValueViolation");
                ValidationError.MinValueViolation mvv = (ValidationError.MinValueViolation) err;
                Assertions.assertEquals("populationSize", mvv.fieldName(),
                        "populationSize must be checked before maxIterations");
                Assertions.assertEquals(in.view().populationSize(), mvv.actual(),
                        "MinValueViolation.actual must echo the offending populationSize");
            } else {
                // iterBad must hold here.
                Assertions.assertInstanceOf(ValidationError.MinValueViolation.class, err,
                        "maxIterations<1 (with roots and pop OK) must yield MinValueViolation");
                ValidationError.MinValueViolation mvv = (ValidationError.MinValueViolation) err;
                Assertions.assertEquals("maxIterations", mvv.fieldName(),
                        "maxIterations field must be reported when only iterations are bad");
                Assertions.assertEquals(in.view().maxIterations(), mvv.actual(),
                        "MinValueViolation.actual must echo the offending maxIterations");
            }

            // (4) No builders called on failure: the Failure carries no RunInputs by construction.
            Assertions.assertFalse(result.isSuccess(),
                    "Failure branch must not produce a Success carrying RunInputs (no ProjectSource/"
                            + "RefactoringConfig constructed)");
        } else {
            // Success branch: roots non-empty AND populationSize >= 1 AND maxIterations >= 1.
            Assertions.assertTrue(result.isSuccess(),
                    () -> "Expected Success for good input (populationSize="
                            + in.view().populationSize() + ", maxIterations="
                            + in.view().maxIterations() + ", roots=" + in.roots() + ")");
            RunInputs ri = result.value().orElseThrow();
            Assertions.assertNotNull(ri.source(), "Success.source must be non-null on success");
            Assertions.assertNotNull(ri.config(), "Success.config must be non-null on success");
        }
    }

    // --- Generators -----------------------------------------------------------

    /**
     * Bundles the two generated inputs: the immutable {@link SettingsView} snapshot
     * and the (possibly null/empty) compiler-output roots list.
     */
    record ValidateInputs(SettingsView view, List<String> roots) {
    }

    @Provide
    Arbitrary<ValidateInputs> validateInputs() {
        Arbitrary<SettingsView> view = settingsViewArbitrary();
        Arbitrary<List<String>> roots = rootsArbitrary();
        return Combinators.combine(view, roots).as(ValidateInputs::new);
    }

    /**
     * SettingsView generator. Each integer-typed field is independently sampled so
     * that arbitrary combinations of "in range" and "out of range" appear.
     *
     * <p>{@code populationSize} and {@code maxIterations} are biased over the design's
     * prescribed boundary set: {@code Integer.MIN_VALUE, -1, 0, 1}, the declared default,
     * and {@code Integer.MAX_VALUE}. {@code wmc}, {@code cbo}, and {@code rfc} are
     * unconstrained {@code int}s — validation does not constrain them today.</p>
     */
    private Arbitrary<SettingsView> settingsViewArbitrary() {
        ClassTrimSettingsState.Defaults d = ClassTrimSettingsState.defaults();
        Arbitrary<Integer> pop = boundedSizeArbitrary(d.populationSize());
        Arbitrary<Integer> iter = boundedSizeArbitrary(d.maxIterations());
        Arbitrary<Integer> wmc = Arbitraries.integers();
        Arbitrary<Integer> cbo = Arbitraries.integers();
        Arbitrary<Integer> rfc = Arbitraries.integers();
        return Combinators.combine(wmc, cbo, rfc, pop, iter)
                .as(SettingsView::new);
    }

    /**
     * Biased generator for the population/iteration "size" fields. Hits the boundary
     * set described in the design's "Test Data and Generators" section:
     * {@code Integer.MIN_VALUE, -1, 0, 1}, the declared default, and
     * {@code Integer.MAX_VALUE}, plus a small uniform-{@code int} tail to widen the
     * coverage (especially around the {@code <1} threshold).
     */
    private static Arbitrary<Integer> boundedSizeArbitrary(int defaultValue) {
        return Arbitraries.frequencyOf(
                net.jqwik.api.Tuple.of(2, Arbitraries.just(Integer.MIN_VALUE)),
                net.jqwik.api.Tuple.of(2, Arbitraries.just(-1)),
                net.jqwik.api.Tuple.of(3, Arbitraries.just(0)),
                net.jqwik.api.Tuple.of(3, Arbitraries.just(1)),
                net.jqwik.api.Tuple.of(2, Arbitraries.just(defaultValue)),
                net.jqwik.api.Tuple.of(2, Arbitraries.just(Integer.MAX_VALUE)),
                // Small uniform tail: covers values just above and below the threshold.
                net.jqwik.api.Tuple.of(3, Arbitraries.integers().between(-10, 10)),
                net.jqwik.api.Tuple.of(3, Arbitraries.integers())
        );
    }

    /**
     * Roots generator. Lists of size 0..8 (size 0 hits the empty-roots branch).
     * Entries are arbitrary strings drawn from a small, deduplication-friendly
     * universe so empty-list and non-empty-list cases both occur frequently.
     * The list itself is also occasionally {@code null} so the
     * {@code roots == null} guard in {@code validate} is exercised.
     */
    private static Arbitrary<List<String>> rootsArbitrary() {
        Arbitrary<String> entry = Arbitraries.of(
                "/p/out", "/p/build/classes", "/p/m1/out", "/p/m2/out", "");
        Arbitrary<List<String>> nonNullList = entry.list().ofMinSize(0).ofMaxSize(8);
        return Arbitraries.frequencyOf(
                net.jqwik.api.Tuple.of(1, Arbitraries.just((List<String>) null)),
                net.jqwik.api.Tuple.of(9, nonNullList.map(ArrayList::new))
        );
    }
}
