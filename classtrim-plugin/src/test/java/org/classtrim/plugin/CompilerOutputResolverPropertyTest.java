package org.classtrim.plugin;

import com.intellij.openapi.vfs.VfsUtilCore;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tag;
import org.junit.jupiter.api.Assertions;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Property-based test for {@link CompilerOutputResolver#resolveFromInputs}.
 *
 * <p>Validates: Requirements 2.1, 2.2, 2.3, 2.4 — the "Compiler-output resolution
 * is idempotent, total, and dedupes existing paths" property from the
 * {@code idea-plugin} design (Correctness Property 1).</p>
 *
 * <p>The test uses a small finite "universe" of candidate path strings so that the
 * existence-set sampling stays tractable. Each generated input draws its strings
 * from this universe (plus the null / blank "no value provided" cases), and the
 * existence predicate is realized as a sampled {@code Set<String>}'s
 * {@code ::contains} method. The oracle re-runs the design's algorithm in plain
 * Java and asserts equality with the resolver's output.</p>
 *
 * <p>Project-URL strings include three shapes: plain paths, {@code "file://"}-
 * prefixed URLs, and {@code "jar://"}-prefixed URLs. The oracle mirrors the
 * trivial {@code VfsUtilCore.urlToPath} semantics ("split on the first {@code
 * "://"}; if absent, return the input unchanged"), so the property covers the
 * composition of URL translation with the surrounding deduplication, existence-
 * filtering, ordering, and fallback rules.</p>
 *
 * <p>The fallback path string {@code "<basePath>/target/classes"} is kept disjoint
 * from the candidate-path universe (the universe contains no path ending in
 * {@code /target/classes}), so any occurrence of the fallback string in the
 * result must have come from the fallback branch — this lets the explicit
 * fallback-rule assertion be unambiguous.</p>
 */
class CompilerOutputResolverPropertyTest {

    /**
     * The candidate-path universe. Deliberately small so the existence subset
     * generator can enumerate it efficiently and the oracle stays simple.
     * None of these paths end in {@code /target/classes} so they cannot collide
     * with the fallback string.
     */
    private static final List<String> PATH_UNIVERSE = List.of(
            "/p/out",
            "/p/build/classes",
            "/p/m1/out/production",
            "/p/m2/out/production",
            "/p/m3/build",
            "/p/m4/bin",
            "/p/m5/classes/main",
            "/p/m6/work"
    );

    /**
     * Base-path universe. Deliberately disjoint from {@link #PATH_UNIVERSE} so the
     * computed fallback string {@code "<basePath>/target/classes"} cannot equal
     * any other generated input.
     */
    private static final List<String> BASE_PATH_UNIVERSE = List.of(
            "/proj/a",
            "/proj/b",
            "/proj/c"
    );

    /**
     * <strong>Feature: idea-plugin, Property 1: Compiler-output resolution is
     * idempotent, total, and dedupes existing paths.</strong>
     *
     * <p>For any tuple {@code (projectUrl, moduleOutputs[], basePath, existsOnDisk)},
     * {@code CompilerOutputResolver.resolveFromInputs(...)} returns a list {@code R}
     * such that:</p>
     * <ol>
     *   <li>every element of {@code R} satisfies {@code existsOnDisk};</li>
     *   <li>every element of {@code R} is non-{@code null} and non-blank;</li>
     *   <li>{@code R} contains no duplicates;</li>
     *   <li>{@code R} preserves first-seen order across project URL → modules in
     *       iteration order → optional fallback;</li>
     *   <li>the fallback {@code <basePath>/target/classes} is added only when
     *       neither {@code projectUrl} nor any module path was provided as a
     *       non-{@code null}/non-blank value, {@code basePath != null}, and the
     *       fallback exists;</li>
     *   <li>the resolver never throws.</li>
     * </ol>
     *
     * <p>Validates: Requirements 2.1, 2.2, 2.3, 2.4.</p>
     */
    @Property(tries = 100)
    // The human-readable tag per the design ("Feature: idea-plugin, Property 1: ...")
    // is published via @Label below. JUnit 5's @Tag does not accept colons or
    // commas, so a sanitized identifier is used here for tooling-level filtering.
    @Tag("idea-plugin-property-1")
    @Label("Feature: idea-plugin, Property 1: Compiler-output resolution is idempotent, total, and dedupes existing paths")
    void resolverPreservesInvariants(
            @ForAll("resolverInputs") ResolverInputs in) {

        Predicate<String> existsOnDisk = in.existingPaths()::contains;

        // (6) Totality: must never throw.
        List<String> result = Assertions.assertDoesNotThrow(
                () -> CompilerOutputResolver.resolveFromInputs(
                        in.projectUrl(),
                        in.moduleOutputs(),
                        in.basePath(),
                        existsOnDisk),
                "resolveFromInputs must never throw");

        // (1) Every entry passes the existence predicate.
        for (String entry : result) {
            Assertions.assertTrue(
                    in.existingPaths().contains(entry),
                    () -> "Result contained '" + entry + "' which is not in the existing-paths set");
        }

        // (2) No null or blank entries.
        for (String entry : result) {
            Assertions.assertNotNull(entry, "Result contained a null entry");
            Assertions.assertFalse(entry.isBlank(), "Result contained a blank entry");
        }

        // (3) No duplicates.
        Assertions.assertEquals(
                result.size(),
                new HashSet<>(result).size(),
                "Result contained duplicate entries");

        // (4) First-seen order + (5) fallback rule: oracle equality.
        List<String> expected = expectedResult(in);
        Assertions.assertEquals(expected, result,
                "Result did not equal the oracle (first-seen order or fallback rule violated)");

        // (5) Explicit fallback rule cross-check (independent of the oracle).
        // Because the fallback string is disjoint from PATH_UNIVERSE, a fallback
        // appearance in the result is unambiguous.
        boolean projectUrlProvided = isProvided(in.projectUrl());
        boolean anyModuleProvided = false;
        if (in.moduleOutputs() != null) {
            for (String m : in.moduleOutputs()) {
                if (isProvided(m)) {
                    anyModuleProvided = true;
                    break;
                }
            }
        }
        boolean fallbackEligible = !projectUrlProvided && !anyModuleProvided;

        String fallbackString = (in.basePath() == null)
                ? null
                : Path.of(in.basePath(), "target", "classes").toString();

        if (!fallbackEligible || in.basePath() == null) {
            // Fallback must NOT appear in the result.
            if (fallbackString != null) {
                Assertions.assertFalse(
                        result.contains(fallbackString),
                        "Fallback path '" + fallbackString
                                + "' appeared in the result even though projectUrlProvided="
                                + projectUrlProvided + " and anyModuleProvided="
                                + anyModuleProvided);
            }
        } else {
            // Fallback eligible: it appears iff it exists.
            boolean fallbackExists = in.existingPaths().contains(fallbackString);
            Assertions.assertEquals(
                    fallbackExists,
                    result.contains(fallbackString),
                    "Fallback presence in result did not match its existence on disk");
        }
    }

    // --- Oracle ---------------------------------------------------------------

    /**
     * Re-runs the design's algorithm in plain Java to produce the expected result
     * list. Mirrors {@link CompilerOutputResolver#resolveFromInputs} step-for-step,
     * including the {@code VfsUtilCore.urlToPath} translation of the project URL
     * (so that {@code "file://"}- and {@code "jar://"}-prefixed inputs collapse
     * to their underlying path before existence-filtering and deduplication).
     */
    private static List<String> expectedResult(ResolverInputs in) {
        LinkedHashSet<String> result = new LinkedHashSet<>();

        boolean projectUrlProvided = isProvided(in.projectUrl());
        boolean anyModuleProvided = false;

        if (projectUrlProvided) {
            String path;
            try {
                path = VfsUtilCore.urlToPath(in.projectUrl());
            } catch (Throwable t) {
                path = null;
            }
            if (path != null && !path.isBlank()) {
                result.add(path);
            }
        }

        if (in.moduleOutputs() != null) {
            for (String modulePath : in.moduleOutputs()) {
                if (modulePath == null || modulePath.isBlank()) {
                    continue;
                }
                anyModuleProvided = true;
                result.add(modulePath);
            }
        }

        result.removeIf(p -> !in.existingPaths().contains(p));

        boolean fallbackEligible = !projectUrlProvided && !anyModuleProvided;
        if (fallbackEligible && in.basePath() != null) {
            String fallback = Path.of(in.basePath(), "target", "classes").toString();
            if (in.existingPaths().contains(fallback)) {
                result.add(fallback);
            }
        }

        return List.copyOf(result);
    }

    private static boolean isProvided(String s) {
        return s != null && !s.isBlank();
    }

    // --- Generators -----------------------------------------------------------

    /**
     * Bundles the four generated inputs (plus the sampled existence set) so that
     * the existence set can be drawn from the same universe as the path inputs.
     */
    record ResolverInputs(
            String projectUrl,
            List<String> moduleOutputs,
            String basePath,
            Set<String> existingPaths) {
    }

    @Provide
    Arbitrary<ResolverInputs> resolverInputs() {
        Arbitrary<String> projectUrl = projectUrlArbitrary();
        Arbitrary<List<String>> moduleOutputs = moduleOutputsArbitrary();
        Arbitrary<String> basePath = basePathArbitrary();

        // The existence set is sampled from the union of every path that could
        // ever appear in the result: PATH_UNIVERSE plus every <basePath>/target/classes
        // string. This gives the predicate the chance to accept or reject each
        // candidate independently per generated example.
        List<String> universeForExistence = new ArrayList<>(PATH_UNIVERSE);
        for (String b : BASE_PATH_UNIVERSE) {
            universeForExistence.add(Path.of(b, "target", "classes").toString());
        }
        Arbitrary<Set<String>> existingPaths =
                Arbitraries.subsetOf(universeForExistence).ofMinSize(0);

        return Combinators.combine(projectUrl, moduleOutputs, basePath, existingPaths)
                .as(ResolverInputs::new);
    }

    /**
     * Project URL: biased to include {@code null}, {@code ""}, {@code "   "}, and
     * three shapes of path-bearing strings drawn from {@link #PATH_UNIVERSE}: the
     * raw path, a {@code "file://"}-prefixed URL, and a {@code "jar://"}-prefixed
     * URL. The oracle resolves URLs through {@code VfsUtilCore.urlToPath} so the
     * property covers the URL → path translation step in addition to the
     * surrounding deduplication, existence-filtering, ordering, and fallback
     * rules.
     */
    private static Arbitrary<String> projectUrlArbitrary() {
        Arbitrary<String> rawPath = Arbitraries.of(PATH_UNIVERSE);
        Arbitrary<String> fileUrl = Arbitraries.of(PATH_UNIVERSE).map(p -> "file://" + p);
        Arbitrary<String> jarUrl  = Arbitraries.of(PATH_UNIVERSE).map(p -> "jar://" + p);
        return Arbitraries.frequencyOf(
                net.jqwik.api.Tuple.of(2, Arbitraries.just((String) null)),
                net.jqwik.api.Tuple.of(1, Arbitraries.just("")),
                net.jqwik.api.Tuple.of(1, Arbitraries.just("   ")),
                net.jqwik.api.Tuple.of(3, rawPath),
                net.jqwik.api.Tuple.of(2, fileUrl),
                net.jqwik.api.Tuple.of(1, jarUrl)
        );
    }

    /**
     * Module outputs: list of length 0–8 where each entry is independently
     * {@code null}, blank, or a path from {@link #PATH_UNIVERSE}. Duplicates and
     * a mix of nullable / valid entries arise naturally from this generator.
     */
    private static Arbitrary<List<String>> moduleOutputsArbitrary() {
        Arbitrary<String> entry = Arbitraries.frequencyOf(
                net.jqwik.api.Tuple.of(2, Arbitraries.just((String) null)),
                net.jqwik.api.Tuple.of(1, Arbitraries.just("")),
                net.jqwik.api.Tuple.of(1, Arbitraries.just("   ")),
                net.jqwik.api.Tuple.of(6, Arbitraries.of(PATH_UNIVERSE))
        );
        // Also generate the null list itself occasionally to exercise the
        // null-list branch of the resolver.
        Arbitrary<List<String>> nonNullList = entry.list().ofMinSize(0).ofMaxSize(8);
        return Arbitraries.frequencyOf(
                net.jqwik.api.Tuple.of(1, Arbitraries.just((List<String>) null)),
                net.jqwik.api.Tuple.of(9, nonNullList)
        );
    }

    /**
     * Base path: biased to include {@code null} and entries drawn from
     * {@link #BASE_PATH_UNIVERSE}.
     */
    private static Arbitrary<String> basePathArbitrary() {
        return Arbitraries.frequencyOf(
                net.jqwik.api.Tuple.of(2, Arbitraries.just((String) null)),
                net.jqwik.api.Tuple.of(8, Arbitraries.of(BASE_PATH_UNIVERSE))
        );
    }

    // --- Defensive: silence "unused import" if a reviewer adds Arrays-based debugging ----
    @SuppressWarnings("unused")
    private static List<String> debugCopy(List<String> in) {
        return Arrays.asList(in.toArray(new String[0]));
    }
}
