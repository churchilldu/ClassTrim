# Implementation Plan: idea-plugin

## Overview

This plan turns the `idea-plugin` design into a sequence of incremental Java coding tasks for the `classtrim-plugin` Maven module. Each task adds a discrete component or test, builds on previous work, and ends by wiring the pieces together through `plugin.xml`. Pure helpers (`CompilerOutputResolver`, `AnalysisRunFactory`, `ClassTrimNotifier` formatters, slot/publication state machines) are implemented first so that they can be exercised by jqwik property tests before the IntelliJ-integration glue is wired up.

The implementation language is **Java** (the design uses concrete Java code samples, package paths, and IntelliJ Platform APIs throughout, so no language-selection step is needed).

## Tasks

- [x] 1. Set up plugin module skeleton and shared types
  - [x] 1.1 Create plugin module skeleton, shared records, and test dependencies
    - Create directory layout under `classtrim-plugin/src/main/java/org/classtrim/plugin/` matching the design's "Module / Package Layout" section (root package, `settings/` subpackage)
    - Add `SettingsView`, `RunInputs`, and the sealed `ValidationError` hierarchy (`MinValueViolation`, `MissingDefault`, `NoCompilerRoots`) as records in `org.classtrim.plugin`
    - Confirm `classtrim-plugin/pom.xml` declares the IntelliJ Platform SDK, a dependency on `classtrim-core`, JUnit 5, and jqwik (test scope); add what is missing
    - _Requirements: 3.5, 3.6, 3.7_

- [x] 2. Implement compiler-output resolution
  - [x] 2.1 Implement `CompilerOutputResolver` pure layer
    - Add `CompilerOutputResolver.resolveFromInputs(String projectOutputUrlOrNull, List<String> moduleOutputPathsRawNullable, String projectBasePathOrNull, Predicate<String> existsOnDisk)` following the algorithm in the design (LinkedHashSet; `VfsUtilCore.urlToPath` for the project URL; skip null/blank module entries; drop entries that fail `existsOnDisk`; fallback to `<basePath>/target/classes` only when neither project URL nor any module path provided a non-null/non-blank value)
    - Return `List.copyOf(...)` preserving discovery order
    - Never throw on any input
    - _Requirements: 2.1, 2.2, 2.3, 2.4_

  - [x] 2.2 Write property test for compiler-output resolver
    - **Property 1: Compiler-output resolution is idempotent, total, and dedupes existing paths**
    - **Validates: Requirements 2.1, 2.2, 2.3, 2.4**
    - jqwik `@Property(tries = 100)` over `(projectUrl, moduleOutputs[], basePath, existsOnDisk)` generators described in the design's "Test Data and Generators" section
    - Assert: every entry passes `existsOnDisk`; no nulls/blanks; no duplicates; first-seen order preserved across project URL → modules → fallback; fallback added only when no project URL and no non-null/non-blank module path was provided; never throws
    - Tag the test method with `Feature: idea-plugin, Property 1: ...`

  - [x] 2.3 Implement `CompilerOutputResolver.resolve(Project)` IntelliJ-facing layer
    - Read `CompilerProjectExtension.getInstance(project).getCompilerOutputUrl()`
    - Iterate `ModuleManager.getInstance(project).getModules()` and read `CompilerModuleExtension.getInstance(module).getCompilerOutputPath()` for each
    - Forward the extracted strings plus `project.getBasePath()` and `Files::exists` into `resolveFromInputs(...)`
    - _Requirements: 2.1, 2.2, 2.3, 2.4_

- [x] 3. Implement settings layer
  - [x] 3.1 Confirm and extend `ClassTrimSettingsState`
    - Verify the existing `@Service(Project)` `PersistentStateComponent<State>` exposes `wmc=8, cbo=8, rfc=30, populationSize=500, maxIterations=2000` defaults as described in the design's Data Models section
    - Add a `SettingsView view()` accessor returning an immutable `SettingsView(wmc, cbo, rfc, populationSize, maxIterations)` snapshot
    - Add a static `Defaults defaults()` accessor used by validation to satisfy R3.4 / R3.6
    - Ensure `updateFrom(...)` (or any future mutator) does not run during validation
    - _Requirements: 3.1, 3.4, 3.6_

  - [x] 3.2 Implement `ClassTrimSettingsConfigurable` UI page
    - Render spinners for `wmc`, `cbo`, `rfc`, `populationSize`, `maxIterations`
    - Set `min=1` on the population-size and max-iterations spinners (UI-level enforcement of R3.5)
    - Persist edits through `ClassTrimSettingsState.updateFrom(...)`
    - _Requirements: 3.1, 3.5_

  - [x] 3.3 Write unit tests for settings persistence and view snapshot
    - Round-trip a `State` through the persistent component and assert field equality
    - Assert `view()` returns a snapshot whose mutation does not affect the persisted state
    - _Requirements: 3.1, 3.4_

- [x] 4. Implement settings-to-config / settings-to-source factory
  - [x] 4.1 Implement `AnalysisRunFactory.buildSource` and `AnalysisRunFactory.buildConfig`
    - `buildSource(String projectName, List<String> roots, Threshold t)` returns `new BinaryPathProjectSource(projectName, roots, t)` with no transformation of any field
    - `buildConfig(Threshold t, int populationSize, int maxIterations)` returns `new RefactoringConfig(t, populationSize, maxIterations)` with no transformation
    - `Threshold` is built directly from `(state.wmc, state.cbo, state.rfc)` with no transformation
    - _Requirements: 3.2, 3.3_

  - [x] 4.2 Write property test for settings-to-config / settings-to-source identity mapping
    - **Property 2: Settings-to-Config / Settings-to-Source mapping is identity, with defaults filling unset fields**
    - **Validates: Requirements 3.2, 3.3, 3.4**
    - jqwik property over `(projectName, roots non-empty, SettingsView with populationSize >= 1 and maxIterations >= 1)`
    - Assert bit-equal `getThreshold()`, `getPopulationSize()`, `getMaxIterations()` on the `RefactoringConfig`
    - Assert `getProjectName()`, element-wise equal `getBinaryRoots()`, and equal `getThreshold()` on the `BinaryPathProjectSource`
    - Generate `SettingsView` instances by overriding an arbitrary subset of fields starting from `Defaults` and assert each output field reflects the override (if present) or the declared default (otherwise)
    - Tag the test method with `Feature: idea-plugin, Property 2: ...`

  - [x] 4.3 Implement `AnalysisRunFactory.validate`
    - Signature: `Result<RunInputs, ValidationError> validate(SettingsView v, List<String> roots, String projectName)`
    - Precedence (deterministic): `NoCompilerRoots` < `MinValueViolation` (and within `MinValueViolation`, `populationSize` is checked before `maxIterations`)
    - Return `MinValueViolation("populationSize", v.populationSize())` when `v.populationSize() < 1`
    - Return `MinValueViolation("maxIterations", v.maxIterations())` when `v.maxIterations() < 1`
    - Return `NoCompilerRoots` when `roots.isEmpty()`
    - On success return a `RunInputs` carrying the `ProjectSource` and `RefactoringConfig` that would be built — but do not construct them when validation fails
    - Never mutate the persisted `ClassTrimSettingsState.State`
    - _Requirements: 3.5, 3.7_

  - [x] 4.4 Write property test for validation
    - **Property 3: Validation rejects bad inputs without mutating persisted settings**
    - **Validates: Requirements 3.5, 3.7**
    - jqwik property over `(SettingsView v, List<String> roots)` with biased generators including `Integer.MIN_VALUE`, `0`, `1`, defaults, `Integer.MAX_VALUE`
    - Assert: bad inputs (`populationSize < 1` OR `maxIterations < 1` OR `roots.isEmpty()`) yield a `ValidationError`
    - Assert: precedence is deterministic in the order `NoCompilerRoots` < `MinValueViolation` (population before iterations)
    - Assert: the persisted `ClassTrimSettingsState.State` is bit-identical before and after the call (use a fresh in-memory `State` snapshot)
    - Assert: no `ProjectSource` and no `RefactoringConfig` is constructed on the failure branch (spy on the factory or assert via a pure variant of `validate` that does not call the builders)
    - Tag the test method with `Feature: idea-plugin, Property 3: ...`

  - [x] 4.5 Write defensive unit test for the missing-default branch
    - Exercise the `MissingDefault` arm of `ValidationError` (defaults accessor returning a sentinel "unset") and assert it surfaces a notification naming the offending setting
    - _Requirements: 3.6_

- [x] 5. Implement notifier formatting helpers
  - [x] 5.1 Implement `ClassTrimNotifier` core wrapper
    - Always resolve the `"ClassTrim Notifications"` group via `NotificationGroupManager.getInstance().getNotificationGroup(...)`
    - Expose `info(Project, String title, String body)`, `warning(...)`, `error(...)` methods
    - _Requirements: 6.1, 6.3, 6.4_

  - [x] 5.2 Implement `ClassTrimNotifier.truncate` and `formatFailureBody`
    - `truncate(String s, int max)` returns `""` when `s == null`, `s` when `s.length() <= max`, otherwise `s.substring(0, max)`
    - `formatFailureBody(String className, String messageOrNull)` returns `className + ": " + truncate(messageOrNull, 500)`
    - Never throw on any input (including `null` message, very long message, embedded newlines, non-ASCII)
    - _Requirements: 6.3_

  - [x] 5.3 Implement success-body formatter
    - Add a `formatSuccessBody(int suggestionCount)` helper whose output contains the decimal representation of `suggestionCount` such that parsing the count out of the body returns the input value
    - When `suggestionCount == 0`, additionally include a phrase indicating that no suggestions were produced
    - _Requirements: 5.2, 5.3_

  - [x] 5.4 Write property test for failure-notification body formatter
    - **Property 7: Failure notification body is well-formed and bounded**
    - **Validates: Requirements 6.3**
    - jqwik property over `(className non-blank, message: String?)` generators biased to include `null`, empty, very long (`> 500` chars), embedded newlines, and non-ASCII
    - Assert: body starts with `className`; the message portion follows immediately; message portion length `<= 500`; equals `message` when `message != null && length <= 500`; equals `message.substring(0, 500)` when longer; never throws
    - Tag the test method with `Feature: idea-plugin, Property 7: ...`

  - [x] 5.5 Write property test for success-notification body formatter
    - **Property 6: Success notification body reports the suggestion count**
    - **Validates: Requirements 5.2, 5.3**
    - jqwik property over `K >= 0` (including `0`, `1`, large `K` close to `Integer.MAX_VALUE`)
    - Assert: parsing the decimal count out of the body returns `K`
    - Assert: when `K == 0`, the body additionally contains a phrase indicating that no suggestions were produced
    - Tag the test method with `Feature: idea-plugin, Property 6: ...`

- [x] 6. Checkpoint - Pure layer complete
  - Ensure all tests pass, ask the user if questions arise.

- [x] 7. Implement run-slot state machine and coordinator skeleton
  - [x] 7.1 Implement `AnalysisCoordinator` slot management
    - Register as `@Service(Project)`
    - Hold `AtomicReference<ProgressIndicator> runningIndicator`
    - `isRunning()` returns `runningIndicator.get() != null`
    - `requestRun()` reserves the slot via `compareAndSet(null, placeholder)`; on `false`, emit info notification "ClassTrim analysis is already in progress" and return without scheduling work; on `true`, proceed
    - Provide a `releaseSlot()` private path called from the task's `onFinished()`
    - Expose `cancelRunning()` that delegates to the held indicator
    - _Requirements: 4.5, 4.7_

  - [x] 7.2 Write property test for run-slot state machine
    - **Property 4: At most one Analysis_Run executes concurrently per project**
    - **Validates: Requirements 4.5, 4.7**
    - jqwik property over random schedules of `acquire`/`release` events (length 1–32) submitted to a model coordinator that does not actually schedule background work
    - Assert: the count of held slots over every prefix of the interleaving is in `{0, 1}`
    - Assert: only the first concurrent `acquire` succeeds; subsequent acquires until the next release return the rejection branch and do not submit a task
    - Tag the test method with `Feature: idea-plugin, Property 4: ...`

  - [x] 7.3 Implement `RunClassTrimAnalysisAction`
    - Extend `AnAction`; declare `getActionUpdateThread() == BGT`
    - `update(AnActionEvent e)`: disable when `e.getProject() == null` OR `AnalysisCoordinator.getInstance(project).isRunning()`
    - `actionPerformed(AnActionEvent e)`: delegate to `AnalysisCoordinator.getInstance(project).requestRun()` and return immediately
    - _Requirements: 1.1, 1.2, 1.3_

  - [x] 7.4 Write IntelliJ light test for action enable/disable wiring
    - Two examples each for R1.2 (no project) and R1.3 (analysis running) using `BasePlatformTestCase`
    - _Requirements: 1.2, 1.3_

- [x] 8. Implement publication state machine
  - [x] 8.1 Implement `ClassTrimToolWindowPanel.updateSuggestions`
    - Hold a `DefaultTableModel` with columns `Method`, `From`, `To`
    - Static `updateSuggestions(Project, List<RefactoringSuggestion>)` clears existing rows and inserts one row per suggestion on the EDT
    - Maintain a `static Map<String, ClassTrimToolWindowPanel> PANELS` keyed by `project.getLocationHash()`
    - Failure / cancellation paths must never call `updateSuggestions`
    - _Requirements: 5.1, 5.5_

  - [x] 8.2 Implement `ClassTrimToolWindowFactory`
    - Register the `"ClassTrim"` tool window
    - Create the panel, register it in `PANELS`, and attach it as the tool-window content
    - _Requirements: 5.1_

  - [x] 8.3 Write property test for publication state machine
    - **Property 5: Tool-window publication state machine**
    - **Validates: Requirements 4.4, 5.1, 5.5**
    - jqwik property over sequences of `Success(L) | Failure | Cancellation` events (where `L` is a generated `List<RefactoringSuggestion>`)
    - Use a model where the published list is replaced only by `Success(L)` and is unchanged by `Failure` or `Cancellation`
    - Assert: after each prefix of events, the published list equals the most recent `Success(L_k)` if any, otherwise the initial empty list
    - Tag the test method with `Feature: idea-plugin, Property 5: ...`

- [x] 9. Wire the analysis pipeline through the coordinator
  - [x] 9.1 Compose the precondition pipeline in `AnalysisCoordinator.requestRun`
    - Order (deterministic, per design):
      1. Project + base path present (else warning "Open a Java project" via `ClassTrimNotifier.warning`, R6.1)
      2. Slot free (else info "Already in progress", R4.7) — already implemented in 7.1
      3. `roots = resolver.resolve(project)`; if empty, release the slot, emit warning "Build the project before running ClassTrim" (R2.5, R3.7)
      4. `validatedSettings = AnalysisRunFactory.validate(settings.view(), roots, project.getName())`; on `MinValueViolation` or `MissingDefault`, release the slot and emit error naming the offending field (R3.5, R3.6)
    - Build `Task.Backgroundable("ClassTrim Analysis", cancellable=true)` and submit it via `ProgressManager.getInstance().run(...)` only after all preconditions pass
    - Capture the real `ProgressIndicator` into `runningIndicator` from inside `Task.Backgroundable.run`
    - _Requirements: 1.1, 1.4, 2.5, 3.1, 3.5, 3.6, 3.7, 4.1, 4.2, 6.1_

  - [x] 9.2 Implement the background task body
    - Inside `Task.Backgroundable.run(ProgressIndicator indicator)`:
      - Construct fresh `StandardProjectAnalyzer + InMemoryProjectRepository + NSGAIIIRefactoringEngine + ClassTrimService` per run
      - `indicator.checkCanceled()` before and after `service.analyze(source, config)`
      - On `ProcessCanceledException`: emit info "ClassTrim analysis was cancelled" (R4.4, R6.4); rethrow so IntelliJ marks the task cancelled
      - On any other `Throwable t`: log with stack trace via `Logger.getInstance(AnalysisCoordinator.class).error(t)` (R6.2); emit error notification with body `formatFailureBody(t.getClass().getName(), t.getMessage())` (R6.3); do not call `updateSuggestions` (R5.5)
      - On success: call `ClassTrimToolWindowPanel.updateSuggestions(project, result.getSuggestions())` and emit info notification with body `formatSuccessBody(result.getSuggestions().size())` (R5.1, R5.2, R5.3)
    - In `onFinished()`: clear `runningIndicator` and refresh the action's enabled state (R4.6)
    - _Requirements: 4.1, 4.3, 4.4, 4.6, 5.1, 5.2, 5.3, 5.4, 5.5, 6.2, 6.3, 6.4_

  - [x] 9.3 Write integration test for end-to-end Analysis_Run
    - Use `HeavyPlatformTestCase` against a synthetic compiled fixture project
    - Assert: progress indicator title is `"ClassTrim Analysis"` (R4.2)
    - Assert: `service.analyze` is invoked off the EDT (R4.1)
    - Assert: suggestions arrive in the tool window after success (R5.1)
    - Assert: cancelling the indicator aborts the run within 1 s and no suggestions are published (R4.3, R4.4, R5.5)
    - Assert: status-bar indicator is removed within 1 s of termination (R4.6)
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.6, 5.1, 5.5_

- [x] 10. Register plugin extensions
  - [x] 10.1 Update `plugin.xml`
    - Register `<action id="ClassTrim.RunAnalysis">` under `ToolsMenu` bound to `RunClassTrimAnalysisAction` (R1.1, R1.2, R1.3)
    - Register `<toolWindow id="ClassTrim">` bound to `ClassTrimToolWindowFactory` (R5.1, R5.5)
    - Register `<projectService>` for `ClassTrimSettingsState` and `AnalysisCoordinator`
    - Register `<projectConfigurable>` for `ClassTrimSettingsConfigurable`
    - Register `<notificationGroup id="ClassTrim Notifications" displayType="BALLOON">` (R6.1, R6.3, R6.4)
    - _Requirements: 1.1, 1.2, 1.3, 3.1, 4.5, 4.7, 5.1, 5.5, 6.1, 6.3, 6.4_

  - [x] 10.2 Write IntelliJ light test for plugin.xml extensions
    - Verify the action is registered, the tool window factory is registered, the notification group exists, and the project services are injectable
    - _Requirements: 1.1, 5.1, 6.1_

- [x] 11. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP. Property tests (Properties 1–7) are sub-tasks of the components they validate so that counter-examples surface as close to the implementation as possible.
- Each task references specific acceptance criteria from `requirements.md` for traceability; property tests additionally cite the design property number and the requirements clause(s) the property validates.
- Pure logic (resolver, factory, validation, notifier formatters, slot/publication state machines) is implemented and property-tested before any IntelliJ-runtime wiring is added, so the bulk of correctness is established without an IntelliJ test fixture.
- Integration / light tests cover the criteria the design's "Test Layers" section flagged as poorly suited to PBT (action availability, ProgressManager wiring, notification group registration, end-to-end timing).
- The implementation language is Java 17+; jqwik is the property-testing library (per the design's "PBT Configuration" section).

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["2.1", "3.1", "4.1", "5.1", "8.1"] },
    { "id": 2, "tasks": ["2.2", "2.3", "3.2", "4.3", "5.2", "7.1", "8.2"] },
    { "id": 3, "tasks": ["3.3", "4.2", "4.4", "4.5", "5.3", "5.4", "7.2", "7.3", "8.3"] },
    { "id": 4, "tasks": ["5.5", "7.4", "9.1"] },
    { "id": 5, "tasks": ["9.2"] },
    { "id": 6, "tasks": ["9.3", "10.1"] },
    { "id": 7, "tasks": ["10.2"] }
  ]
}
```
