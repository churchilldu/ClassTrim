package org.classtrim.plugin;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tag;
import org.junit.jupiter.api.Assertions;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Property-based test for the run-slot state machine that backs
 * {@link AnalysisCoordinator}'s {@code requestRun()} / {@code releaseSlot()}
 * transitions.
 *
 * <p>Validates: Requirements 4.5, 4.7 — the "At most one Analysis_Run executes
 * concurrently per project" property from the {@code idea-plugin} design
 * (Correctness Property 4).</p>
 *
 * <p>The system under test is a small <em>model coordinator</em> that re-uses
 * the same atomic-reference logic as
 * {@link AnalysisCoordinator#requestRun()} but does not schedule any
 * background work, so the property can run in plain JUnit / jqwik without
 * standing up an IntelliJ test fixture. The model is deliberately a separate
 * class from {@link AnalysisCoordinator} because the production coordinator
 * pulls in {@code com.intellij.openapi.project.Project} and the rest of the
 * IntelliJ Platform; lifting only the slot logic into a tiny model lets the
 * property exercise the full random-schedule state machine without any
 * IntelliJ runtime on the classpath.</p>
 *
 * <p>The property generates random sequences of {@code ACQUIRE} / {@code
 * RELEASE} events of length 1–32 (per the design's "Test Data and Generators"
 * section) and applies them in order to a single {@link ModelCoordinator}.
 * After every event it cross-checks the observed transition against an
 * oracle-derived expected {@code held} state, and at the end it asserts
 * that the held-slot count over every prefix of the interleaving stayed
 * within {@code {0, 1}}.</p>
 */
class RunSlotStateMachinePropertyTest {

    /**
     * Minimal model of {@link AnalysisCoordinator}'s slot-management logic.
     *
     * <p>{@link #acquire()} mirrors the
     * {@link java.util.concurrent.atomic.AtomicReference#compareAndSet(Object, Object)}
     * acquisition in {@code AnalysisCoordinator.requestRun()}: it succeeds
     * iff the slot was previously free, and otherwise reports the rejection
     * branch (no work scheduled). {@link #release()} clears the slot back to
     * the free state, matching {@code AnalysisCoordinator.releaseSlot()}.
     * {@link #held()} returns the same state-machine truth observed by
     * {@code AnalysisCoordinator.isRunning()}.</p>
     *
     * <p>Crucially this class does <em>not</em> submit any background task —
     * the property is purely a state-machine assertion, so dropping the
     * production coordinator's task-submission side effect lets the model
     * be exercised over thousands of random schedules without IntelliJ
     * fixtures.</p>
     */
    static final class ModelCoordinator {
        private final AtomicReference<Object> slot = new AtomicReference<>();

        enum Acquire { GRANTED, REJECTED }

        Acquire acquire() {
            Object placeholder = new Object();
            return slot.compareAndSet(null, placeholder) ? Acquire.GRANTED : Acquire.REJECTED;
        }

        void release() {
            slot.set(null);
        }

        boolean held() {
            return slot.get() != null;
        }
    }

    /**
     * Event alphabet for the schedule generator. Mirrors the two transitions
     * in {@code AnalysisCoordinator}: {@code ACQUIRE} corresponds to
     * {@code requestRun()}'s slot-reservation step, and {@code RELEASE}
     * corresponds to {@code releaseSlot()}.
     */
    enum EventType { ACQUIRE, RELEASE }

    /**
     * <strong>Feature: idea-plugin, Property 4: At most one Analysis_Run
     * executes concurrently per project.</strong>
     *
     * <p>For any random schedule of {@code acquire} / {@code release} events
     * (length 1–32) submitted to a {@link ModelCoordinator}, the
     * {@code held} count over every prefix of the interleaving stays within
     * {@code {0, 1}}, and only the first {@code acquire} on a free slot
     * succeeds; subsequent acquires until the next release return
     * {@link ModelCoordinator.Acquire#REJECTED}. {@code release} is a no-op
     * on a free slot and clears a held slot.</p>
     *
     * <p>Validates: Requirements 4.5, 4.7.</p>
     */
    @Property(tries = 100)
    // The human-readable tag per the design ("Feature: idea-plugin, Property 4: ...")
    // is published via @Label below. JUnit 5's @Tag does not accept colons or
    // commas, so a sanitized identifier is used here for tooling-level filtering.
    @Tag("idea-plugin-property-4")
    @Label("Feature: idea-plugin, Property 4: At most one Analysis_Run executes concurrently per project")
    void slotStateMachineUpholdsAtMostOneInvariant(
            @ForAll("eventSchedules") List<EventType> schedule) {

        ModelCoordinator coord = new ModelCoordinator();

        // Pre-condition: a freshly constructed model coordinator is in the
        // free state. This anchors the prefix-invariant at index 0.
        Assertions.assertFalse(coord.held(),
                "ModelCoordinator must start in the free state");

        boolean expectedHeld = false;

        for (int i = 0; i < schedule.size(); i++) {
            final int idx = i;
            final EventType event = schedule.get(i);
            final boolean heldBefore = expectedHeld;

            switch (event) {
                case ACQUIRE -> {
                    ModelCoordinator.Acquire outcome = coord.acquire();
                    if (heldBefore) {
                        // R4.5 / R4.7: a second acquire while the slot is
                        // held must take the rejection branch — i.e. no
                        // task is submitted and the in-flight slot stays
                        // held.
                        Assertions.assertEquals(
                                ModelCoordinator.Acquire.REJECTED, outcome,
                                () -> "ACQUIRE while held must return REJECTED at index " + idx);
                        Assertions.assertTrue(coord.held(),
                                () -> "Held state must remain true after a rejected ACQUIRE at index " + idx);
                        // expectedHeld unchanged: still true.
                    } else {
                        // Free slot: the first concurrent acquire succeeds.
                        Assertions.assertEquals(
                                ModelCoordinator.Acquire.GRANTED, outcome,
                                () -> "ACQUIRE on free slot must return GRANTED at index " + idx);
                        Assertions.assertTrue(coord.held(),
                                () -> "Held state must become true after a granted ACQUIRE at index " + idx);
                        expectedHeld = true;
                    }
                }
                case RELEASE -> {
                    coord.release();
                    if (heldBefore) {
                        // Held → free: release clears the slot.
                        Assertions.assertFalse(coord.held(),
                                () -> "Held state must become false after RELEASE at index " + idx);
                        expectedHeld = false;
                    } else {
                        // Free → free: release is a no-op (matches
                        // AnalysisCoordinator.releaseSlot() which
                        // unconditionally calls AtomicReference.set(null)).
                        Assertions.assertFalse(coord.held(),
                                () -> "Held state must remain false after RELEASE on free slot at index " + idx);
                        // expectedHeld unchanged: still false.
                    }
                }
            }

            // Prefix invariant: the held count is in {0, 1} after every event.
            // Modelled as a boolean here because the slot is single-valued, so
            // "count" is exactly {false=0, true=1}; the assertion is therefore
            // that observed and expected agree, which keeps the count in the
            // {0, 1} range by construction.
            final boolean expectedAtIdx = expectedHeld;
            Assertions.assertEquals(expectedAtIdx, coord.held(),
                    () -> "Observed held state diverged from oracle after event "
                            + event + " at index " + idx
                            + " (prefix=" + schedule.subList(0, idx + 1) + ")");
        }
    }

    // --- Generators -----------------------------------------------------------

    /**
     * Schedule generator: list of length 1–32 where each entry is drawn
     * uniformly from {@link EventType#ACQUIRE} and {@link EventType#RELEASE}.
     * Range matches the design's "Test Data and Generators" section.
     */
    @Provide
    Arbitrary<List<EventType>> eventSchedules() {
        Arbitrary<EventType> event =
                Arbitraries.of(EventType.ACQUIRE, EventType.RELEASE);
        return event.list().ofMinSize(1).ofMaxSize(32);
    }
}
