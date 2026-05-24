package org.classtrim.plugin;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tag;
import net.jqwik.api.constraints.IntRange;
import org.classtrim.core.metric.Threshold;
import org.classtrim.core.config.RefactoringConfig;
import org.classtrim.core.model.BinaryPathProjectSource;
import org.classtrim.core.model.ProjectSource;
import org.classtrim.plugin.settings.ClassTrimSettingsState;
import org.junit.jupiter.api.Assertions;

import java.util.ArrayList;
import java.util.List;

/**
 * Property-based test for {@link AnalysisRunFactory#buildSource(String, java.util.List, Threshold)},
 * {@link AnalysisRunFactory#buildConfig(Threshold, int, int)}, and
 * {@link AnalysisRunFactory#threshold(SettingsView)}.
 *
 * <p>Validates: Requirements 3.2, 3.3, 3.4 — the "Settings-to-Config / Settings-to-Source
 * mapping is identity, with defaults filling unset fields" property from the
 * {@code idea-plugin} design (Correctness Property 2).</p>
 *
 * <p>The single property method generates a {@link SettingsView} by independently choosing,
 * for each of {@code wmc}, {@code cbo}, {@code rfc}, {@code populationSize}, and
 * {@code maxIterations}, between (a) the declared default from
 * {@link ClassTrimSettingsState#defaults()} and (b) a freshly generated arbitrary integer
 * (with {@code populationSize} and {@code maxIterations} restricted to {@code [1, INT_MAX]}
 * to satisfy R3.5). It then asserts:</p>
 *
 * <ul>
 *   <li>bit-equal {@code getThreshold()}, {@code getPopulationSize()},
 *       {@code getMaxIterations()} on the produced {@link RefactoringConfig};</li>
 *   <li>{@code getProjectName()}, element-wise equal {@code getBinaryRoots()}, and
 *       equal {@code getThreshold()} on the produced {@link BinaryPathProjectSource};</li>
 *   <li>each output field reflects the override (when chosen) or the declared default
 *       (otherwise).</li>
 * </ul>
 *
 * <p>The test deliberately stays inside the pure plugin layer: it never instantiates
 * {@code ClassTrimSettingsState} (only its static {@code defaults()} accessor is invoked),
 * never touches {@code ProgressManager}, and never schedules background work.</p>
 */
class AnalysisRunFactoryIdentityMappingPropertyTest {

    /**
     * <strong>Feature: idea-plugin, Property 2: Settings-to-Config / Settings-to-Source
     * mapping is identity, with defaults filling unset fields.</strong>
     *
     * <p>For any {@code (projectName, roots non-empty, SettingsView v)} where
     * {@code v.populationSize() >= 1} and {@code v.maxIterations() >= 1}:</p>
     *
     * <ul>
     *   <li>{@code buildConfig(threshold(v), v.populationSize(), v.maxIterations())} returns
     *       a {@link RefactoringConfig} whose {@code getThreshold()} (and its
     *       {@code WMC/CBO/RFC} components), {@code getPopulationSize()}, and
     *       {@code getMaxIterations()} are bit-equal to the corresponding view fields;</li>
     *   <li>{@code buildSource(projectName, roots, threshold(v))} returns a
     *       {@link BinaryPathProjectSource} whose {@code getProjectName()} equals
     *       {@code projectName}, whose {@code getBinaryRoots()} is element-wise equal to
     *       {@code roots}, and whose {@code getThreshold()} equals the threshold built
     *       from {@code v};</li>
     *   <li>when the per-field generator chose to omit an override, the corresponding
     *       output field equals the declared default from
     *       {@link ClassTrimSettingsState#defaults()}; when it chose an override, the
     *       output field equals the override.</li>
     * </ul>
     *
     * <p>Validates: Requirements 3.2, 3.3, 3.4.</p>
     */
    @Property(tries = 100)
    // JUnit 5's @Tag does not accept colons or commas; the human-readable label is on
    // @Label below. The sanitized identifier here is used for tooling-level filtering.
    @Tag("idea-plugin-property-2")
    @Label("Feature: idea-plugin, Property 2: Settings-to-Config / Settings-to-Source mapping is identity, with defaults filling unset fields")
    void mappingIsIdentityWithDefaultsFilling(
            @ForAll("projectNames") String projectName,
            @ForAll("nonEmptyRoots") List<String> roots,
            @ForAll("fieldChoice") FieldChoice wmcChoice,
            @ForAll("fieldChoice") FieldChoice cboChoice,
            @ForAll("fieldChoice") FieldChoice rfcChoice,
            @ForAll("positiveFieldChoice") FieldChoice popChoice,
            @ForAll("positiveFieldChoice") FieldChoice maxIterChoice) {

        ClassTrimSettingsState.Defaults d = ClassTrimSettingsState.defaults();

        // Per-field "use default vs. use override" selection. The expected value matches
        // exactly what we will feed into the SettingsView, which makes the assertions
        // below "output field reflects override-or-default" by construction.
        int expectedWmc = wmcChoice.useOverride() ? wmcChoice.overrideValue() : d.wmc();
        int expectedCbo = cboChoice.useOverride() ? cboChoice.overrideValue() : d.cbo();
        int expectedRfc = rfcChoice.useOverride() ? rfcChoice.overrideValue() : d.rfc();
        int expectedPop = popChoice.useOverride() ? popChoice.overrideValue() : d.populationSize();
        int expectedMaxIter =
                maxIterChoice.useOverride() ? maxIterChoice.overrideValue() : d.maxIterations();

        // Defaults themselves must satisfy the precondition populationSize >= 1 and
        // maxIterations >= 1 from R3.5; positiveFieldChoice ensures every override does
        // too. The assertions guard against a regression in the declared defaults.
        Assertions.assertTrue(expectedPop >= 1,
                "test setup invariant: populationSize must be >= 1");
        Assertions.assertTrue(expectedMaxIter >= 1,
                "test setup invariant: maxIterations must be >= 1");

        SettingsView view = new SettingsView(
                expectedWmc, expectedCbo, expectedRfc, expectedPop, expectedMaxIter);

        // (a) threshold(view) is built from the view's WMC/CBO/RFC with no transformation.
        Threshold t = AnalysisRunFactory.threshold(view);
        Assertions.assertEquals(expectedWmc, t.getWMC(), "threshold WMC reflects view");
        Assertions.assertEquals(expectedCbo, t.getCBO(), "threshold CBO reflects view");
        Assertions.assertEquals(expectedRfc, t.getRFC(), "threshold RFC reflects view");

        // (b) RefactoringConfig identity: bit-equal getters; getThreshold() returns the
        // exact instance handed in (no defensive copy).
        RefactoringConfig config = AnalysisRunFactory.buildConfig(t, expectedPop, expectedMaxIter);
        Assertions.assertSame(t, config.getThreshold(),
                "config.getThreshold() must be the same Threshold instance passed in");
        Assertions.assertEquals(expectedWmc, config.getThreshold().getWMC(),
                "config.getThreshold().getWMC() must equal the input view's wmc");
        Assertions.assertEquals(expectedCbo, config.getThreshold().getCBO(),
                "config.getThreshold().getCBO() must equal the input view's cbo");
        Assertions.assertEquals(expectedRfc, config.getThreshold().getRFC(),
                "config.getThreshold().getRFC() must equal the input view's rfc");
        Assertions.assertEquals(expectedPop, config.getPopulationSize(),
                "config.getPopulationSize() must equal the input view's populationSize");
        Assertions.assertEquals(expectedMaxIter, config.getMaxIterations(),
                "config.getMaxIterations() must equal the input view's maxIterations");

        // (c) BinaryPathProjectSource identity: getProjectName / getBinaryRoots /
        // getThreshold reflect the inputs unchanged.
        ProjectSource src = AnalysisRunFactory.buildSource(projectName, roots, t);
        Assertions.assertTrue(src instanceof BinaryPathProjectSource,
                "buildSource must return a BinaryPathProjectSource");
        BinaryPathProjectSource bpps = (BinaryPathProjectSource) src;
        Assertions.assertEquals(projectName, bpps.getProjectName(),
                "source.getProjectName() must equal the input projectName");
        Assertions.assertEquals(roots, new ArrayList<>(bpps.getBinaryRoots()),
                "source.getBinaryRoots() must be element-wise equal to the input roots");
        Assertions.assertSame(t, bpps.getThreshold(),
                "source.getThreshold() must be the same Threshold instance passed in");

        // (d) Defaults-fill cross-check: when no field is overridden, every output equals
        // the declared default. This is the same assertion as (b)+(c) but specialized to
        // the all-default mask, included to make a regression in defaults() obvious.
        if (!wmcChoice.useOverride()
                && !cboChoice.useOverride()
                && !rfcChoice.useOverride()
                && !popChoice.useOverride()
                && !maxIterChoice.useOverride()) {
            Assertions.assertEquals(d.wmc(), config.getThreshold().getWMC(),
                    "all-defaults: config WMC must equal Defaults.wmc()");
            Assertions.assertEquals(d.cbo(), config.getThreshold().getCBO(),
                    "all-defaults: config CBO must equal Defaults.cbo()");
            Assertions.assertEquals(d.rfc(), config.getThreshold().getRFC(),
                    "all-defaults: config RFC must equal Defaults.rfc()");
            Assertions.assertEquals(d.populationSize(), config.getPopulationSize(),
                    "all-defaults: config populationSize must equal Defaults.populationSize()");
            Assertions.assertEquals(d.maxIterations(), config.getMaxIterations(),
                    "all-defaults: config maxIterations must equal Defaults.maxIterations()");
        }
    }

    // --- Generators -----------------------------------------------------------

    /**
     * Project names: short non-blank-or-blank strings. The factory does not validate the
     * project name in any way (R3.3 is identity-of-storage), so this generator only
     * exercises the identity contract of
     * {@link BinaryPathProjectSource#getProjectName()}.
     */
    @Provide
    Arbitrary<String> projectNames() {
        return Arbitraries.strings().ofMinLength(1).ofMaxLength(20);
    }

    /**
     * Non-empty list of root strings. Roots are non-null because
     * {@link BinaryPathProjectSource}'s constructor calls {@link List#copyOf}, which
     * rejects nulls — the property is about the identity mapping for <em>valid</em>
     * roots only.
     */
    @Provide
    Arbitrary<List<String>> nonEmptyRoots() {
        return Arbitraries.strings().ofMaxLength(20).list().ofMinSize(1).ofMaxSize(8);
    }

    /**
     * Per-field choice generator for {@code wmc}, {@code cbo}, and {@code rfc}: the
     * override value may be any {@code int} (including {@code 0}, negatives, and
     * {@link Integer#MIN_VALUE}/{@link Integer#MAX_VALUE} via jqwik's edge-case
     * sampling). The selection between "use default" and "use override" is uniform.
     */
    @Provide
    Arbitrary<FieldChoice> fieldChoice() {
        Arbitrary<Boolean> useOverride = Arbitraries.of(true, false);
        Arbitrary<Integer> override = Arbitraries.integers();
        return Combinators.combine(useOverride, override).as(FieldChoice::new);
    }

    /**
     * Per-field choice generator for {@code populationSize} and {@code maxIterations}:
     * identical to {@link #fieldChoice()} but the override range is restricted to
     * {@code [1, Integer.MAX_VALUE]} per the task's "populationSize >= 1 and
     * maxIterations >= 1" precondition (R3.5).
     */
    @Provide
    Arbitrary<FieldChoice> positiveFieldChoice() {
        Arbitrary<Boolean> useOverride = Arbitraries.of(true, false);
        Arbitrary<Integer> override =
                Arbitraries.integers().between(1, Integer.MAX_VALUE);
        return Combinators.combine(useOverride, override).as(FieldChoice::new);
    }

    /**
     * Per-field selection: when {@link #useOverride()} is {@code true}, the
     * {@link SettingsView} field takes {@link #overrideValue()}; otherwise it takes the
     * corresponding default from {@link ClassTrimSettingsState#defaults()}.
     */
    public record FieldChoice(boolean useOverride, int overrideValue) {
    }
}
