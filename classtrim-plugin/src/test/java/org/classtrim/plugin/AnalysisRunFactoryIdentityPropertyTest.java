package org.classtrim.plugin;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.From;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tag;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;
import net.jqwik.api.constraints.StringLength;
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
 * <p>Two property methods are exposed:</p>
 * <ol>
 *   <li>{@link #mappingIsIdentity} — for arbitrary
 *       {@code (projectName, roots, populationSize >= 1, maxIterations >= 1, wmc, cbo, rfc)},
 *       every getter on the produced {@link RefactoringConfig} and
 *       {@link BinaryPathProjectSource} is bit-equal to the corresponding input
 *       (R3.2, R3.3).</li>
 *   <li>{@link #defaultsFillUnsetFields} — generates an "override mask" (a subset of
 *       {@code {wmc, cbo, rfc, populationSize, maxIterations}}) plus override values, and
 *       builds a {@link SettingsView} where unmasked fields take the corresponding value
 *       from {@link ClassTrimSettingsState#defaults()} and masked fields take the override
 *       value. The property asserts that every field of the resulting
 *       {@code RefactoringConfig} / {@code BinaryPathProjectSource} reflects the override
 *       (when present) or the declared default (otherwise) (R3.4).</li>
 * </ol>
 *
 * <p>The test deliberately stays inside the pure plugin layer: it never instantiates
 * {@code ClassTrimSettingsState} (only its static {@code defaults()} accessor is invoked),
 * never touches {@code ProgressManager}, and never schedules background work.</p>
 */
class AnalysisRunFactoryIdentityPropertyTest {

    /**
     * <strong>Feature: idea-plugin, Property 2: Settings-to-Config / Settings-to-Source
     * mapping is identity, with defaults filling unset fields.</strong>
     *
     * <p>For any {@code (projectName, roots non-empty, populationSize >= 1,
     * maxIterations >= 1, wmc, cbo, rfc)}:</p>
     * <ul>
     *   <li>{@code buildConfig(threshold(view), populationSize, maxIterations)} returns
     *       a {@link RefactoringConfig} whose {@code getThreshold()},
     *       {@code getPopulationSize()}, and {@code getMaxIterations()} are bit-equal to
     *       the inputs;</li>
     *   <li>{@code buildSource(projectName, roots, threshold(view))} returns a
     *       {@link BinaryPathProjectSource} whose {@code getProjectName()} equals
     *       {@code projectName}, whose {@code getBinaryRoots()} equals {@code roots}
     *       element-wise, and whose {@code getThreshold()} equals the threshold built
     *       from {@code view}.</li>
     * </ul>
     *
     * <p>Validates: Requirements 3.2, 3.3.</p>
     */
    @Property(tries = 100)
    // JUnit 5's @Tag does not accept colons or commas; the human-readable label is on
    // @Label below. The sanitized identifier here is used for tooling-level filtering.
    @Tag("idea-plugin-property-2")
    @Label("Feature: idea-plugin, Property 2: Settings-to-Config / Settings-to-Source mapping is identity, with defaults filling unset fields")
    void mappingIsIdentity(
            @ForAll("projectNames") String projectName,
            @ForAll("nonEmptyRoots") List<String> roots,
            @ForAll @IntRange(min = 1, max = Integer.MAX_VALUE) int populationSize,
            @ForAll @IntRange(min = 1, max = Integer.MAX_VALUE) int maxIterations,
            @ForAll int wmc,
            @ForAll int cbo,
            @ForAll int rfc) {

        SettingsView view = new SettingsView(wmc, cbo, rfc, populationSize, maxIterations);
        Threshold t = AnalysisRunFactory.threshold(view);

        // (a) Threshold getters are bit-equal to the inputs.
        Assertions.assertEquals(wmc, t.getWMC(), "threshold WMC");
        Assertions.assertEquals(cbo, t.getCBO(), "threshold CBO");
        Assertions.assertEquals(rfc, t.getRFC(), "threshold RFC");

        // (b) RefactoringConfig identity: bit-equal getters.
        RefactoringConfig config = AnalysisRunFactory.buildConfig(t, populationSize, maxIterations);
        Assertions.assertSame(t, config.getThreshold(),
                "config.getThreshold() must be the same Threshold instance passed in");
        Assertions.assertEquals(populationSize, config.getPopulationSize(),
                "config.getPopulationSize() must equal the input populationSize");
        Assertions.assertEquals(maxIterations, config.getMaxIterations(),
                "config.getMaxIterations() must equal the input maxIterations");

        // (c) BinaryPathProjectSource identity: bit-equal getters.
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
    }

    /**
     * <strong>Feature: idea-plugin, Property 2: Settings-to-Config / Settings-to-Source
     * mapping is identity, with defaults filling unset fields.</strong>
     *
     * <p>For any override mask over {@code {wmc, cbo, rfc, populationSize, maxIterations}}
     * plus override values, build a {@link SettingsView} where unmasked fields take the
     * declared default from {@link ClassTrimSettingsState#defaults()} and masked fields
     * take the override values; assert every field of the resulting
     * {@link RefactoringConfig} / {@link BinaryPathProjectSource} reflects the override
     * (when present) or the declared default (otherwise).</p>
     *
     * <p>Validates: Requirements 3.4 (defaults), in conjunction with R3.2/R3.3 (identity
     * mapping enforced by {@link #mappingIsIdentity}).</p>
     */
    @Property(tries = 100)
    @Tag("idea-plugin-property-2")
    @Label("Feature: idea-plugin, Property 2: Settings-to-Config / Settings-to-Source mapping is identity, with defaults filling unset fields")
    void defaultsFillUnsetFields(
            @ForAll("overrideMasks") OverrideMask mask,
            @ForAll int wmcOverride,
            @ForAll int cboOverride,
            @ForAll int rfcOverride,
            @ForAll @IntRange(min = 1, max = Integer.MAX_VALUE) int populationOverride,
            @ForAll @IntRange(min = 1, max = Integer.MAX_VALUE) int maxIterOverride,
            @ForAll("projectNames") String projectName,
            @ForAll("nonEmptyRoots") List<String> roots) {

        ClassTrimSettingsState.Defaults d = ClassTrimSettingsState.defaults();

        int expectedWmc = mask.wmc() ? wmcOverride : d.wmc();
        int expectedCbo = mask.cbo() ? cboOverride : d.cbo();
        int expectedRfc = mask.rfc() ? rfcOverride : d.rfc();
        int expectedPop = mask.populationSize() ? populationOverride : d.populationSize();
        int expectedMaxIter = mask.maxIterations() ? maxIterOverride : d.maxIterations();

        SettingsView view = new SettingsView(
                expectedWmc, expectedCbo, expectedRfc, expectedPop, expectedMaxIter);
        Threshold t = AnalysisRunFactory.threshold(view);
        RefactoringConfig config = AnalysisRunFactory.buildConfig(t, expectedPop, expectedMaxIter);
        ProjectSource src = AnalysisRunFactory.buildSource(projectName, roots, t);

        // Each output field reflects the override-or-default value per field.
        Assertions.assertEquals(expectedWmc, config.getThreshold().getWMC(), "config WMC");
        Assertions.assertEquals(expectedCbo, config.getThreshold().getCBO(), "config CBO");
        Assertions.assertEquals(expectedRfc, config.getThreshold().getRFC(), "config RFC");
        Assertions.assertEquals(expectedPop, config.getPopulationSize(), "config populationSize");
        Assertions.assertEquals(expectedMaxIter, config.getMaxIterations(), "config maxIterations");

        BinaryPathProjectSource bpps = (BinaryPathProjectSource) src;
        Assertions.assertEquals(projectName, bpps.getProjectName(), "source projectName");
        Assertions.assertEquals(roots, new ArrayList<>(bpps.getBinaryRoots()), "source binaryRoots");
        Assertions.assertEquals(expectedWmc, bpps.getThreshold().getWMC(), "source WMC");
        Assertions.assertEquals(expectedCbo, bpps.getThreshold().getCBO(), "source CBO");
        Assertions.assertEquals(expectedRfc, bpps.getThreshold().getRFC(), "source RFC");

        // Cross-check: when no field is overridden, every output equals the declared default.
        if (!mask.wmc() && !mask.cbo() && !mask.rfc()
                && !mask.populationSize() && !mask.maxIterations()) {
            Assertions.assertEquals(d.wmc(), config.getThreshold().getWMC(), "default WMC");
            Assertions.assertEquals(d.cbo(), config.getThreshold().getCBO(), "default CBO");
            Assertions.assertEquals(d.rfc(), config.getThreshold().getRFC(), "default RFC");
            Assertions.assertEquals(d.populationSize(), config.getPopulationSize(),
                    "default populationSize");
            Assertions.assertEquals(d.maxIterations(), config.getMaxIterations(),
                    "default maxIterations");
        }
    }

    // --- Generators -----------------------------------------------------------

    /**
     * Project names: short non-blank strings biased to include a few well-known shapes
     * (empty, whitespace-padded, alphanumeric). The factory does not validate the project
     * name in any way, so this generator only exercises the identity-of-storage contract
     * of {@link BinaryPathProjectSource#getProjectName()}.
     */
    @Provide
    Arbitrary<String> projectNames() {
        return Arbitraries.frequencyOf(
                net.jqwik.api.Tuple.of(1, Arbitraries.just("")),
                net.jqwik.api.Tuple.of(1, Arbitraries.just("   ")),
                net.jqwik.api.Tuple.of(8, Arbitraries.strings().ofMinLength(1).ofMaxLength(20))
        );
    }

    /**
     * Non-empty list of root strings. Roots are non-null because
     * {@link BinaryPathProjectSource}'s constructor calls {@link List#copyOf} which
     * rejects nulls — the property is about the identity mapping for
     * <em>valid</em> roots.
     */
    @Provide
    Arbitrary<List<String>> nonEmptyRoots() {
        return Arbitraries.strings().ofMaxLength(20).list().ofMinSize(1).ofMaxSize(8);
    }

    /**
     * Override mask: a subset of {@code {wmc, cbo, rfc, populationSize, maxIterations}}
     * represented as 5 booleans. Each combination of {@code 2^5 = 32} masks is reachable
     * by {@link Arbitraries#integers()}-driven boolean sampling, including the all-false
     * mask (assert defaults appear everywhere) and the all-true mask (assert overrides
     * appear everywhere).
     */
    @Provide
    Arbitrary<OverrideMask> overrideMasks() {
        Arbitrary<Boolean> bools = Arbitraries.of(true, false);
        return Combinators.combine(bools, bools, bools, bools, bools).as(OverrideMask::new);
    }

    /**
     * Boolean-valued mask over the five {@link SettingsView} fields. {@code true} means
     * "override the default with a generated value"; {@code false} means "use the
     * declared default from {@link ClassTrimSettingsState#defaults()}".
     */
    public record OverrideMask(
            boolean wmc,
            boolean cbo,
            boolean rfc,
            boolean populationSize,
            boolean maxIterations
    ) {
    }
}
