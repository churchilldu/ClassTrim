package org.classtrim.plugin;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.TestActionEvent;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

/**
 * IntelliJ light test that wires the {@link RunClassTrimAnalysisAction#update}
 * path through real platform machinery (no mocked {@code AnActionEvent}). The
 * test class extends
 * {@link com.intellij.testFramework.fixtures.BasePlatformTestCase}, which
 * stands up the lightweight in-memory project fixture supplied by the
 * IntelliJ test framework — light enough to exercise the action's enable /
 * disable contract against a real {@link Project} service container without
 * paying the cost of {@code HeavyPlatformTestCase}.
 *
 * <p>This task only requires that the file compiles against the IntelliJ test
 * classpath; the surefire run is not part of 7.4 (the design schedules
 * end-to-end runtime coverage in tasks 9.3 and 10.2). JUnit 3-style naming
 * ({@code public void testXxx()}) is mandatory because
 * {@link BasePlatformTestCase} ultimately extends
 * {@link junit.framework.TestCase}.
 *
 * <h2>Coverage</h2>
 * Two examples each for the two acceptance criteria the action's
 * {@code update} method enforces:
 * <ul>
 *   <li>R1.2 — action is disabled while no IDE_Project is open
 *       ({@code e.getProject() == null}).</li>
 *   <li>R1.3 — action is disabled while an Analysis_Run is already executing
 *       for the IDE_Project (driven through the package-private
 *       {@link AnalysisCoordinator#setRunningIndicator(com.intellij.openapi.progress.ProgressIndicator)}
 *       seam added in task 7.1; cleaned up via
 *       {@link AnalysisCoordinator#releaseSlot()}).</li>
 * </ul>
 *
 * <p>The R1.3 cases mutate a process-wide singleton (the project-level
 * {@link AnalysisCoordinator} service), so each one releases the slot in a
 * {@code finally} block to leave the lightweight test fixture in a clean
 * state for any subsequent test methods that share the same project.
 */
public class RunClassTrimAnalysisActionLightTest extends BasePlatformTestCase {

    // ---------------------------------------------------------------------
    // R1.2 — action is disabled when no project is open.
    // Two examples cover the two ways an empty / unbound DataContext arises
    // in the IntelliJ test framework: the no-arg TestActionEvent factory and
    // an explicitly-passed DataContext.EMPTY_CONTEXT.
    // ---------------------------------------------------------------------

    /**
     * R1.2 — Example 1: building an {@link AnActionEvent} with an
     * explicitly-empty {@link DataContext} produces an event whose
     * {@link AnActionEvent#getProject()} returns {@code null}, and the
     * action's {@code update} must therefore disable the presentation.
     *
     * <p>Note: the no-arg {@link TestActionEvent#createTestEvent()} factory
     * picks up an ambient {@link Project} from the lightweight fixture's
     * data context, so it is not suitable for "no project" cases. Both
     * examples here build the event with {@link DataContext#EMPTY_CONTEXT}.</p>
     */
    public void testActionDisabledWhenNoProject() {
        RunClassTrimAnalysisAction action = new RunClassTrimAnalysisAction();
        AnActionEvent event = TestActionEvent.createTestEvent(action, DataContext.EMPTY_CONTEXT);

        action.update(event);

        assertFalse(
                "R1.2: action must be disabled when no IDE_Project is open",
                event.getPresentation().isEnabled());
    }

    /**
     * R1.2 — Example 2: invoking {@code update} a second time on an event
     * whose context still has no project must keep the action disabled.
     * Defends against a regression where {@code update} caches a stale
     * "enabled" state on the presentation and skips the {@code project ==
     * null} branch on subsequent calls.
     */
    public void testActionRemainsDisabledAcrossRepeatedUpdateWithoutProject() {
        RunClassTrimAnalysisAction action = new RunClassTrimAnalysisAction();
        AnActionEvent event = TestActionEvent.createTestEvent(action, DataContext.EMPTY_CONTEXT);

        // Pre-seed the presentation in the "enabled" state so the test
        // observes update() flipping it back to disabled rather than the
        // default false.
        event.getPresentation().setEnabled(true);

        action.update(event);
        assertFalse(
                "R1.2: first update without a project must disable the action",
                event.getPresentation().isEnabled());

        // Re-enable and update a second time to defend against caching.
        event.getPresentation().setEnabled(true);
        action.update(event);
        assertFalse(
                "R1.2: repeated update without a project must keep the action disabled",
                event.getPresentation().isEnabled());
    }

    // ---------------------------------------------------------------------
    // R1.3 — action is disabled while an Analysis_Run is in flight for the
    // IDE_Project. Both examples reserve the slot via the package-private
    // setRunningIndicator(...) seam added in task 7.1 and release it in a
    // finally block so the shared coordinator service is left clean.
    // ---------------------------------------------------------------------

    /**
     * R1.3 — Example 1: with the run slot held, a freshly built
     * {@link AnActionEvent} carrying the test fixture's project must be
     * disabled by {@link RunClassTrimAnalysisAction#update}.
     */
    public void testActionDisabledWhenAnalysisRunning() {
        Project project = getProject();
        AnalysisCoordinator coordinator = AnalysisCoordinator.getInstance(project);

        // Drive the slot state machine into "held" without scheduling a real
        // background task — the indicator is the ownership token observed by
        // AnalysisCoordinator.isRunning(), so installing any non-null
        // indicator is sufficient to assert the R1.3 branch.
        coordinator.setRunningIndicator(new EmptyProgressIndicator());
        try {
            RunClassTrimAnalysisAction action = new RunClassTrimAnalysisAction();
            DataContext dataContext = SimpleDataContext.getProjectContext(project);
            AnActionEvent event = TestActionEvent.createTestEvent(action, dataContext);

            action.update(event);

            assertFalse(
                    "R1.3: action must be disabled while an Analysis_Run is in flight",
                    event.getPresentation().isEnabled());
        } finally {
            // Restore the shared service to the free state so subsequent
            // tests in this class (and any later light tests reusing the
            // same lightweight project) start from a clean slot.
            coordinator.releaseSlot();
        }
    }

    /**
     * R1.3 — Example 2: re-invoking {@code update} on the same event while
     * the slot is held must keep the action disabled. Defends against a
     * regression where {@code update} short-circuits enablement on a cached
     * presentation rather than re-reading
     * {@link AnalysisCoordinator#isRunning()}.
     */
    public void testActionRemainsDisabledAcrossRepeatedUpdateWhileRunning() {
        Project project = getProject();
        AnalysisCoordinator coordinator = AnalysisCoordinator.getInstance(project);

        coordinator.setRunningIndicator(new EmptyProgressIndicator());
        try {
            RunClassTrimAnalysisAction action = new RunClassTrimAnalysisAction();
            DataContext dataContext = SimpleDataContext.getProjectContext(project);
            AnActionEvent event = TestActionEvent.createTestEvent(action, dataContext);

            // First update establishes disabled state under R1.3.
            action.update(event);
            assertFalse(
                    "R1.3: first update while running must disable the action",
                    event.getPresentation().isEnabled());

            // Second update must not re-enable while the slot is still held.
            action.update(event);
            assertFalse(
                    "R1.3: repeated update while running must keep the action disabled",
                    event.getPresentation().isEnabled());
        } finally {
            coordinator.releaseSlot();
        }
    }
}
