package org.classtrim.plugin;

/**
 * Immutable snapshot of {@code ClassTrimSettingsState} used by the pure logic layer
 * (validation, run-inputs construction). The view never references IntelliJ types so
 * it can be exercised by jqwik property tests without an IntelliJ test fixture.
 *
 * <p>Field order matches the on-disk persisted state: {@code wmc, cbo, rfc} are the
 * metric thresholds; {@code populationSize} and {@code maxIterations} are the NSGA-III
 * algorithm parameters.</p>
 */
public record SettingsView(
        int wmc,
        int cbo,
        int rfc,
        int populationSize,
        int maxIterations
) {
}
