package org.classtrim.plugin.settings;

import org.classtrim.plugin.SettingsView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * Unit tests for {@link ClassTrimSettingsState}.
 *
 * <p>These tests exercise the state-only paths of the persistent component
 * (no IntelliJ test fixture required) and validate:</p>
 * <ul>
 *   <li>R3.1 — Plugin_Settings round-trips through
 *       {@link ClassTrimSettingsState#loadState(ClassTrimSettingsState.State)}
 *       and {@link ClassTrimSettingsState#getState()} without mutation.</li>
 *   <li>R3.4 — {@link ClassTrimSettingsState#view()} returns an immutable
 *       snapshot whose previously captured values are decoupled from later
 *       mutations through {@link ClassTrimSettingsState#updateFrom(int, int, int, int, int)}.</li>
 * </ul>
 */
class ClassTrimSettingsStateTest {

    @Test
    @DisplayName("loadState(state) followed by getState() preserves every persisted field (R3.1)")
    void roundTripPreservesAllFields() {
        ClassTrimSettingsState component = new ClassTrimSettingsState();

        ClassTrimSettingsState.State input = new ClassTrimSettingsState.State();
        input.wmc = 1;
        input.cbo = 2;
        input.rfc = 3;
        input.populationSize = 100;
        input.maxIterations = 200;

        component.loadState(input);
        ClassTrimSettingsState.State persisted = component.getState();

        assertNotNull(persisted, "getState() must return the loaded state, not null");
        assertAll("every field must round-trip identically",
                () -> assertEquals(1, persisted.wmc, "wmc"),
                () -> assertEquals(2, persisted.cbo, "cbo"),
                () -> assertEquals(3, persisted.rfc, "rfc"),
                () -> assertEquals(100, persisted.populationSize, "populationSize"),
                () -> assertEquals(200, persisted.maxIterations, "maxIterations")
        );
    }

    @Test
    @DisplayName("view() returns an immutable snapshot decoupled from later mutations (R3.4)")
    void viewSnapshotIsImmutableAndDecoupledFromState() {
        ClassTrimSettingsState component = new ClassTrimSettingsState();

        ClassTrimSettingsState.State input = new ClassTrimSettingsState.State();
        input.wmc = 5;
        input.cbo = 6;
        input.rfc = 7;
        input.populationSize = 50;
        input.maxIterations = 75;
        component.loadState(input);

        SettingsView capturedBefore = component.view();

        // Mutate the persisted state through the plugin's mutator. The previously
        // captured view must not observe these changes — it is a snapshot.
        component.updateFrom(99, 99, 99, 99, 99);

        assertAll("captured snapshot must retain the values seen at the time of view()",
                () -> assertEquals(5, capturedBefore.wmc(), "wmc"),
                () -> assertEquals(6, capturedBefore.cbo(), "cbo"),
                () -> assertEquals(7, capturedBefore.rfc(), "rfc"),
                () -> assertEquals(50, capturedBefore.populationSize(), "populationSize"),
                () -> assertEquals(75, capturedBefore.maxIterations(), "maxIterations")
        );

        // A view taken after the mutation must reflect the new persisted state, and
        // must be a distinct instance from the earlier snapshot.
        SettingsView capturedAfter = component.view();
        assertNotSame(capturedBefore, capturedAfter, "view() must return a fresh snapshot per call");
        assertAll("a view taken after updateFrom(...) must reflect the new state",
                () -> assertEquals(99, capturedAfter.wmc(), "wmc"),
                () -> assertEquals(99, capturedAfter.cbo(), "cbo"),
                () -> assertEquals(99, capturedAfter.rfc(), "rfc"),
                () -> assertEquals(99, capturedAfter.populationSize(), "populationSize"),
                () -> assertEquals(99, capturedAfter.maxIterations(), "maxIterations")
        );
    }
}
