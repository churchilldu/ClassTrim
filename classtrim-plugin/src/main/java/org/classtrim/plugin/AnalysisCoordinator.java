package org.classtrim.plugin;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.classtrim.core.analyzer.StandardProjectAnalyzer;
import org.classtrim.core.engine.NSGAIIIRefactoringEngine;
import org.classtrim.core.engine.RefactoringResult;
import org.classtrim.core.repository.InMemoryProjectRepository;
import org.classtrim.core.service.ClassTrimService;
import org.classtrim.plugin.settings.ClassTrimSettingsState;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Project-level service that owns the single in-flight Analysis_Run slot and
 * composes the precondition pipeline that gates every Analysis_Run.
 *
 * <p>This class is the single source of truth for "is an Analysis_Run currently
 * executing for this project?" — it drives action enable/disable (R1.3) and the
 * duplicate-trigger rejection that satisfies R4.5 and R4.7. The slot itself is
 * an {@link AtomicReference} holding the run's {@link ProgressIndicator}; a
 * non-{@code null} reference means the slot is held, and {@code null} means the
 * slot is free.
 *
 * <p>Acquisition uses {@link AtomicReference#compareAndSet(Object, Object)} so
 * that, in any interleaving of concurrent {@link #requestRun()} invocations,
 * only the first thread observes a free slot and reserves it; every other
 * concurrent caller is rejected with an info notification through the
 * {@code "ClassTrim Notifications"} group and returns without scheduling work.
 *
 * <p>Task 9.1 wires the deterministic precondition pipeline that runs before
 * any background work is scheduled:
 * <ol>
 *   <li>Project + base path present (else warning "Open a Java project to run
 *       ClassTrim", R6.1).</li>
 *   <li>Slot free (else info "ClassTrim analysis is already in progress",
 *       R4.5/R4.7) — implemented via {@link AtomicReference#compareAndSet}.</li>
 *   <li>{@link CompilerOutputResolver#resolve(Project)}; if the resolved roots
 *       are empty release the slot and emit a warning "Build the project before
 *       running ClassTrim" (R2.5/R3.7).</li>
 *   <li>{@link AnalysisRunFactory#validate(SettingsView, List, String)}; on
 *       {@link ValidationError.MinValueViolation} or
 *       {@link ValidationError.MissingDefault} release the slot and emit an
 *       error notification naming the offending field (R3.5/R3.6).</li>
 * </ol>
 *
 * <p>Only after every precondition passes is a
 * {@link Task.Backgroundable Task.Backgroundable("ClassTrim Analysis", cancellable=true)}
 * built and submitted via {@link ProgressManager#run(Task)}, satisfying the
 * "off the EDT" and "status-bar progress" requirements (R4.1/R4.2). The body
 * of that background task — analyze, publish, notify — wires
 * {@link ClassTrimService} (built per run from a fresh
 * {@link StandardProjectAnalyzer} + {@link InMemoryProjectRepository} +
 * {@link NSGAIIIRefactoringEngine}) to
 * {@link ClassTrimToolWindowPanel#updateSuggestions(Project, List)} on
 * success, with deterministic cancellation (R4.4, R6.4) and failure
 * (R6.2, R6.3) paths that never publish partial suggestions (R5.5).
 */
@Service(Service.Level.PROJECT)
public final class AnalysisCoordinator {

    private static final Logger LOG = Logger.getInstance(AnalysisCoordinator.class);

    /**
     * Info-notification body emitted when a second {@link #requestRun()}
     * arrives while a prior run still holds the slot. Required by R4.7.
     */
    static final String ALREADY_IN_PROGRESS_MESSAGE =
            "ClassTrim analysis is already in progress";

    /**
     * Warning-notification body emitted when {@link #requestRun()} is
     * triggered without an open Java project (no {@code Project} or no
     * base path). Required by R6.1.
     */
    static final String OPEN_JAVA_PROJECT_MESSAGE =
            "Open a Java project to run ClassTrim";

    /**
     * Warning-notification body emitted when no compiler-output roots can be
     * resolved for the project — typically because the user has not yet
     * built the project. Required by R2.5 / R3.7.
     */
    static final String BUILD_BEFORE_RUN_MESSAGE =
            "Build the project before running ClassTrim";

    /**
     * Title of the {@link Task.Backgroundable} submitted to
     * {@link ProgressManager}. Surfaced verbatim in the IDE status bar (R4.2).
     */
    static final String BACKGROUND_TASK_TITLE = "ClassTrim Analysis";

    /**
     * Notification title used for every notification dispatched from this
     * coordinator. Centralised so that the precondition-failure messages all
     * surface under the same "ClassTrim" banner.
     */
    static final String NOTIFICATION_TITLE = "ClassTrim";

    /**
     * Info-notification body emitted when an Analysis_Run is cancelled by the
     * developer. Required by R4.4 / R6.4 — the body must indicate that the
     * analysis was cancelled.
     */
    static final String ANALYSIS_CANCELLED_MESSAGE =
            "ClassTrim analysis was cancelled";

    private final Project project;

    /**
     * Notifier used to surface every coordinator notification (precondition
     * failures, slot rejection, future success / failure / cancellation).
     * Injected through the package-private test-seam constructor so that
     * jqwik tests for Property 4 can observe rejections without standing up
     * the IntelliJ {@link com.intellij.notification.NotificationGroupManager}.
     */
    private final ClassTrimNotifier notifier;

    /**
     * Slot reference. {@code null} means "no run in flight"; a non-{@code null}
     * value means "this indicator's run currently holds the slot". All
     * mutations go through {@link AtomicReference#compareAndSet(Object, Object)}
     * (acquisition) or {@link AtomicReference#set(Object)} (release) so that
     * the read in {@link #isRunning()} and the acquisition in
     * {@link #requestRun()} agree on a single, atomic transition.
     */
    private final AtomicReference<ProgressIndicator> runningIndicator = new AtomicReference<>();

    /**
     * Constructed by the platform via the project-level service container.
     * Delegates to the test-seam constructor with the shared
     * {@link ClassTrimNotifier} singleton so that the production wiring uses
     * the real {@code "ClassTrim Notifications"} group.
     */
    public AnalysisCoordinator(Project project) {
        this(project, ClassTrimNotifier.getInstance());
    }

    /**
     * Test seam: allows jqwik tests for Property 4 (run-slot state machine)
     * to inject a stub {@link ClassTrimNotifier} that records rejection
     * notifications without dispatching them through the IntelliJ platform.
     * Package-private so the seam stays scoped to {@code org.classtrim.plugin}.
     */
    AnalysisCoordinator(Project project, ClassTrimNotifier notifier) {
        this.project = project;
        this.notifier = notifier;
    }

    /**
     * Returns the {@link AnalysisCoordinator} bound to {@code project}.
     */
    public static AnalysisCoordinator getInstance(Project project) {
        return project.getService(AnalysisCoordinator.class);
    }

    /**
     * Returns {@code true} when a run currently holds the slot.
     *
     * <p>Used by {@code RunClassTrimAnalysisAction.update(...)} to disable the
     * action while a run is in flight (R1.3) and by tests to observe the
     * state-machine transitions without scheduling background work.
     */
    public boolean isRunning() {
        return runningIndicator.get() != null;
    }

    /**
     * Entry point invoked by {@code RunClassTrimAnalysisAction.actionPerformed}.
     *
     * <p>Composes the precondition pipeline described on the class Javadoc
     * (project + base path → slot reservation → compiler-root resolution →
     * settings validation) and, only when every step succeeds, submits a
     * cancellable {@link Task.Backgroundable} through
     * {@link ProgressManager#run(Task)} (R4.1, R4.2). On any precondition
     * failure the slot is released so that the action becomes clickable again
     * and an appropriately-typed notification is emitted. The method never
     * throws: any unexpected exception from a precondition step is caught,
     * the slot is released, and the failure is surfaced as an error
     * notification (defensive — keeps the action enabled even when the IDE
     * surfaces an unexpected error).
     *
     * <p>The background task body that captures the real
     * {@link ProgressIndicator} into {@link #runningIndicator}, runs
     * {@link ClassTrimService#analyze}, publishes suggestions, and emits the
     * success / failure / cancellation notifications is implemented inline
     * below (task 9.2).
     */
    public void requestRun() {
        runInternal(null);
    }

    /**
     * Module-scoped variant of {@link #requestRun()} invoked by the right-click
     * "Run ClassTrim Analysis" action on a module (or any file inside it) in
     * the Project view. The compiler-root resolution step is replaced with
     * {@link CompilerOutputResolver#resolve(Module)} so only that module's
     * production output ends up in the analyzed roots; every other
     * precondition (project + base path, slot reservation, settings
     * validation) and the entire background-task body are shared with the
     * project-wide path.
     *
     * @param module the module to scope the analysis to. When {@code null},
     *               this method behaves identically to {@link #requestRun()}.
     */
    public void requestRun(Module module) {
        runInternal(module);
    }

    /**
     * Shared implementation of the project-wide and module-scoped run paths.
     * The only difference between the two is the compiler-root resolver call;
     * everything else (slot reservation, settings validation, background-task
     * body, success / failure / cancellation notifications) is identical.
     */
    private void runInternal(Module moduleScope) {
        // (1) Project + base path check (R6.1). Done before slot reservation
        // so that triggering the action without an open Java project is
        // a no-op rather than something that briefly toggles the slot.
        if (project == null || project.getBasePath() == null) {
            notifier.warning(project, NOTIFICATION_TITLE, OPEN_JAVA_PROJECT_MESSAGE);
            return;
        }

        boolean debug = ClassTrimSettingsState.getInstance(project).isDebugEnabled();
        ClassTrimConsole console = ClassTrimConsole.getInstance(project);
        if (debug) {
            LOG.info("[ClassTrim DEBUG] requestRun called. moduleScope="
                    + (moduleScope == null ? "<project-wide>" : moduleScope.getName()));
            console.log("requestRun called. moduleScope="
                    + (moduleScope == null ? "<project-wide>" : moduleScope.getName()));
        }

        // (2) Slot reservation (R4.5/R4.7). Reserve with a placeholder
        // indicator; the real ProgressIndicator replaces the placeholder
        // from inside Task.Backgroundable.run via setRunningIndicator(...).
        ProgressIndicator placeholder = new EmptyProgressIndicator();
        if (!runningIndicator.compareAndSet(null, placeholder)) {
            notifier.info(project, NOTIFICATION_TITLE, ALREADY_IN_PROGRESS_MESSAGE);
            return;
        }

        try {
            // (3) Compiler-root resolution (R2.5/R3.7). When a module scope
            // is supplied, only that module's output is collected — the
            // project-level URL and the <basePath>/target/classes fallback
            // are skipped. Otherwise, the project-wide resolver runs as
            // before.
            List<String> roots = (moduleScope == null)
                    ? CompilerOutputResolver.resolve(project)
                    : CompilerOutputResolver.resolve(moduleScope);
            if (debug) {
                LOG.info("[ClassTrim DEBUG] Resolved roots: " + roots);
                console.log("Resolved roots: " + roots);
            }
            if (roots.isEmpty()) {
                String body = (moduleScope == null)
                        ? BUILD_BEFORE_RUN_MESSAGE
                        : "Build module '" + moduleScope.getName()
                                + "' before running ClassTrim";
                notifier.warning(project, NOTIFICATION_TITLE, body);
                releaseSlot();
                return;
            }

            // (4) Settings validation (R3.5/R3.6). validate(...) never mutates
            // the persisted state and returns a typed ValidationError on
            // failure; the helper below maps that error to a human-readable
            // notification body. The "project name" we pass into the source
            // is the module name when scoped, so suggestions surface under
            // a meaningful label in the tool window.
            ClassTrimSettingsState settings = ClassTrimSettingsState.getInstance(project);
            String sourceName = (moduleScope == null)
                    ? project.getName()
                    : project.getName() + ":" + moduleScope.getName();
            if (debug) {
                LOG.info("[ClassTrim DEBUG] sourceName=" + sourceName
                        + ", settings=" + settings.view());
                console.log("sourceName=" + sourceName + ", settings=" + settings.view());
            }
            Result<RunInputs, ValidationError> result =
                    AnalysisRunFactory.validate(settings.view(), roots, sourceName,
                            settings.isUseGuidingObjectives());
            if (result.isFailure()) {
                ValidationError err = result.error().orElseThrow();
                if (debug) {
                    LOG.info("[ClassTrim DEBUG] Validation failed: " + err);
                    console.log("Validation failed: " + err);
                }
                notifier.error(project, NOTIFICATION_TITLE, formatValidationError(err));
                releaseSlot();
                return;
            }
            RunInputs inputs = result.value().orElseThrow();
            if (debug) {
                LOG.info("[ClassTrim DEBUG] Validation passed. Scheduling background task.");
                console.log("Validation passed. Scheduling background task.");
            }

            // (5) Schedule the background task.
            String taskTitle = (moduleScope == null)
                    ? BACKGROUND_TASK_TITLE
                    : BACKGROUND_TASK_TITLE + " — " + moduleScope.getName();
            Task.Backgroundable task = new Task.Backgroundable(project, taskTitle, true) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    setRunningIndicator(indicator);

                    try {
                        indicator.checkCanceled();

                        if (debug) {
                            LOG.info("[ClassTrim DEBUG] Constructing service stack...");
                            console.log("Constructing service stack...");
                        }
                        InMemoryProjectRepository repository = new InMemoryProjectRepository();
                        StandardProjectAnalyzer analyzer = new StandardProjectAnalyzer(repository);
                        NSGAIIIRefactoringEngine engine = new NSGAIIIRefactoringEngine();
                        ClassTrimService service = new ClassTrimService(analyzer, engine);

                        if (debug) {
                            LOG.info("[ClassTrim DEBUG] Starting analysis...");
                            console.log("Starting analysis...");
                        }
                        long startMs = System.currentTimeMillis();
                        RefactoringResult result = service.analyze(inputs.source(), inputs.config());
                        long elapsedMs = System.currentTimeMillis() - startMs;

                        indicator.checkCanceled();

                        if (debug) {
                            LOG.info("[ClassTrim DEBUG] Analysis completed in " + elapsedMs
                                    + " ms. Suggestions: " + result.getSuggestions().size());
                            console.log("Analysis completed in " + elapsedMs
                                    + " ms. Suggestions: " + result.getSuggestions().size());
                        }

                        ClassTrimToolWindowPanel.updateSuggestions(project, result.getSuggestions());
                        notifier.info(project, NOTIFICATION_TITLE,
                                ClassTrimNotifier.formatSuccessBody(result.getSuggestions().size()));
                    } catch (ProcessCanceledException pce) {
                        if (debug) {
                            LOG.info("[ClassTrim DEBUG] Analysis cancelled by user.");
                            console.log("Analysis cancelled by user.");
                        }
                        notifier.info(project, NOTIFICATION_TITLE, ANALYSIS_CANCELLED_MESSAGE);
                        throw pce;
                    } catch (Throwable t) {
                        if (debug) {
                            LOG.info("[ClassTrim DEBUG] Analysis failed: "
                                    + t.getClass().getName() + ": " + t.getMessage());
                            console.error("Analysis failed: "
                                    + t.getClass().getName() + ": " + t.getMessage());
                        }
                        Logger.getInstance(AnalysisCoordinator.class).error(t);
                        notifier.error(project, NOTIFICATION_TITLE,
                                ClassTrimNotifier.formatFailureBody(
                                        t.getClass().getName(), t.getMessage()));
                    }
                }

                @Override
                public void onFinished() {
                    if (debug) {
                        LOG.info("[ClassTrim DEBUG] Task finished. Releasing slot.");
                        console.log("Task finished. Releasing slot.");
                    }
                    releaseSlot();
                }
            };
            ProgressManager.getInstance().run(task);
        } catch (Throwable t) {
            if (debug) {
                LOG.info("[ClassTrim DEBUG] Precondition pipeline threw: "
                        + t.getClass().getName() + ": " + t.getMessage());
                console.error("Precondition pipeline threw: "
                        + t.getClass().getName() + ": " + t.getMessage());
            }
            releaseSlot();
            notifier.error(project, NOTIFICATION_TITLE,
                    ClassTrimNotifier.formatFailureBody(t.getClass().getName(), t.getMessage()));
        }
    }

    /**
     * Maps a {@link ValidationError} to the human-readable notification body
     * required by R3.5 / R3.6. Each branch names the offending logical field
     * so that the developer can locate the setting in the configurable UI
     * without consulting the log.
     */
    private static String formatValidationError(ValidationError err) {
        if (err instanceof ValidationError.MinValueViolation mvv) {
            return "Setting '" + mvv.fieldName() + "' must be at least 1 (was " + mvv.actual() + ")";
        } else if (err instanceof ValidationError.MissingDefault md) {
            return "No default value available for setting '" + md.fieldName() + "'";
        } else if (err instanceof ValidationError.NoCompilerRoots) {
            return "No compiler output roots were found";
        }
        return "Unknown validation error";
    }

    /**
     * Releases the run slot. Invoked from {@code Task.Backgroundable.onFinished()}
     * once the background task terminates by completion, failure, or
     * cancellation (R4.6).
     *
     * <p>Package-private rather than {@code private} so that
     * jqwik tests in the {@code org.classtrim.plugin} package can drive this
     * state machine directly without standing up an IntelliJ test fixture
     * (Property 4 in task 7.2).
     */
    void releaseSlot() {
        runningIndicator.set(null);
    }

    /**
     * Cancels the currently running task, if any. The held
     * {@link ProgressIndicator} is the run's cancellation handle, so
     * delegating to {@link ProgressIndicator#cancel()} satisfies R4.3 by
     * propagating cancellation into {@code service.analyze(...)}.
     *
     * <p>Calling this when no run is in flight is a no-op.
     */
    public void cancelRunning() {
        ProgressIndicator current = runningIndicator.get();
        if (current != null) {
            current.cancel();
        }
    }

    /**
     * Replaces the placeholder indicator stored by {@link #requestRun()} with
     * the real {@link ProgressIndicator} supplied to
     * {@code Task.Backgroundable.run(ProgressIndicator)}. Called from inside
     * the background task body so that {@link #cancelRunning()} delegates
     * cancellation to the actual indicator owned by the IntelliJ progress
     * manager rather than to the disposable placeholder reserved at
     * slot-acquisition time.
     *
     * <p>Package-private to scope the seam to the {@code org.classtrim.plugin}
     * package: {@code AnalysisCoordinator} is the only legitimate caller, and
     * tests for the slot state machine (Property 4) need to drive the
     * indicator without spinning up an IntelliJ fixture.
     */
    void setRunningIndicator(ProgressIndicator indicator) {
        runningIndicator.set(indicator);
    }
}
