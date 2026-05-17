# Requirements Document

## Introduction

The `idea-plugin` feature delivers an IntelliJ IDEA plugin that lets a developer run the existing ClassTrim NSGA-III move-method refactoring engine against the project currently open in the IDE. The plugin acts as a thin IDE shell over the `classtrim-core` `ClassTrimService` API: it discovers the compiled class roots of the open project, reads the developer's configured analysis parameters, runs the analysis off the event-dispatch thread, and surfaces the resulting move-method suggestions and lifecycle events back to the user.

The scope of this feature is limited to triggering and reporting an analysis run. Rich result rendering (sortable table, navigation), applying suggestions through IntelliJ's refactoring framework, batch apply, baseline-tool comparison, and export are explicitly out of scope and tracked as separate features.

## Glossary

- **ClassTrim_Plugin**: The IntelliJ IDEA plugin produced by the `classtrim-plugin` Maven module.
- **IDE_Project**: The Java project currently open in the IntelliJ IDEA window where the developer invokes the plugin.
- **ClassTrim_Service**: The `org.classtrim.core.service.ClassTrimService` entry point that performs analysis given a `ProjectSource` and `RefactoringConfig`.
- **Project_Source**: An instance of `org.classtrim.core.model.ProjectSource` describing the project name, compiled class roots, and thresholds passed into `ClassTrim_Service`.
- **Refactoring_Config**: An instance of `org.classtrim.core.config.RefactoringConfig` carrying thresholds, population size, and maximum iteration count.
- **Compiler_Output_Roots**: The list of filesystem directories that contain compiled `.class` files for the IDE_Project, derived from project-level and module-level IntelliJ compiler output settings.
- **Plugin_Settings**: The per-project, persisted configuration owned by the plugin (algorithm parameters and metric thresholds).
- **Move_Method_Suggestion**: A single recommendation produced by `ClassTrim_Service` describing a method to relocate from a source class to a target class.
- **Analysis_Run**: A single end-to-end execution of `ClassTrim_Service.analyze` triggered by the developer through the ClassTrim_Plugin.
- **Tool_Window**: The "ClassTrim" tool window registered by the ClassTrim_Plugin where suggestion lists are published.

## Requirements

### Requirement 1: Invoke Analysis From the IDE

**User Story:** As an IntelliJ developer, I want to trigger a ClassTrim NSGA-III analysis from inside the IDE, so that I receive move-method suggestions without leaving my editor.

#### Acceptance Criteria

1. WHEN the developer selects the "Run ClassTrim Analysis" action from the Tools menu, THE ClassTrim_Plugin SHALL start exactly one Analysis_Run for the IDE_Project and SHALL display a progress indicator for that Analysis_Run within 2 seconds of the selection.
2. WHILE no IDE_Project is open in the active IDE window, THE ClassTrim_Plugin SHALL render the "Run ClassTrim Analysis" action in a disabled state that does not respond to mouse clicks or keyboard shortcuts.
3. WHILE an Analysis_Run is already executing for the IDE_Project, THE ClassTrim_Plugin SHALL render the "Run ClassTrim Analysis" action in a disabled state that does not respond to mouse clicks or keyboard shortcuts.
4. IF the ClassTrim_Plugin cannot start the Analysis_Run after the developer selects the "Run ClassTrim Analysis" action, THEN THE ClassTrim_Plugin SHALL display an error notification indicating the cause and SHALL leave the IDE_Project unchanged.

### Requirement 2: Resolve Compiled Class Roots From the IDE_Project

**User Story:** As an IntelliJ developer, I want the plugin to use my project's existing compiled classes as input, so that I do not have to configure binary paths manually.

#### Acceptance Criteria

1. WHEN an Analysis_Run starts, THE ClassTrim_Plugin SHALL collect Compiler_Output_Roots from the IDE_Project by reading the project-level compiler output URL and the production compiler output path of each module, adding each unique resolved file system path to Compiler_Output_Roots exactly once.
2. IF a module's compiler output path is absent, null, or an empty string while collecting Compiler_Output_Roots, THEN THE ClassTrim_Plugin SHALL skip that module's entry, continue collecting outputs from the remaining modules, and raise no error to the developer.
3. WHERE the IDE_Project defines no project-level compiler output URL and no module-level compiler output paths, WHEN an Analysis_Run starts and the directory `<project_base_path>/target/classes` exists on disk, THE ClassTrim_Plugin SHALL add `<project_base_path>/target/classes` to Compiler_Output_Roots.
4. WHEN an Analysis_Run starts, THE ClassTrim_Plugin SHALL exclude from Compiler_Output_Roots any path whose resolved file system location does not exist on disk at the moment the collection completes.
5. IF the resulting Compiler_Output_Roots list contains zero entries after collection completes, THEN THE ClassTrim_Plugin SHALL cancel the Analysis_Run before any class is analyzed and display a warning notification indicating that the developer must build the project before running analysis.

### Requirement 3: Read Analysis Parameters From Plugin Settings

**User Story:** As an IntelliJ developer, I want to control NSGA-III parameters and metric thresholds, so that the analysis matches my project's characteristics.

#### Acceptance Criteria

1. WHEN an Analysis_Run starts, THE ClassTrim_Plugin SHALL read the Plugin_Settings for the IDE_Project from the per-project settings service before constructing the Refactoring_Config or the Project_Source.
2. WHEN the ClassTrim_Plugin constructs the Refactoring_Config for an Analysis_Run, THE ClassTrim_Plugin SHALL set its threshold, population size, and maximum iteration count to the values read in criterion 1 with no transformation.
3. WHEN the ClassTrim_Plugin constructs the Project_Source for an Analysis_Run, THE ClassTrim_Plugin SHALL set the project name to the IDE_Project name, the binary paths to the Compiler_Output_Roots resolved for that Analysis_Run, and the threshold to the Plugin_Settings threshold read in criterion 1, with no transformation.
4. IF a Plugin_Settings field has no developer-assigned value persisted when an Analysis_Run starts, THEN THE ClassTrim_Plugin SHALL use the default value declared by Plugin_Settings for that field.
5. IF the Plugin_Settings population size is less than 1 or the Plugin_Settings maximum iteration count is less than 1 when the Analysis_Run starts, THEN THE ClassTrim_Plugin SHALL cancel the Analysis_Run before constructing the Refactoring_Config, leave the persisted Plugin_Settings unchanged, and display an error notification that names the offending field and states that its minimum value is 1.
6. IF the ClassTrim_Plugin cannot retrieve a default value for a Plugin_Settings field that has no developer-assigned value persisted, THEN THE ClassTrim_Plugin SHALL cancel the Analysis_Run before constructing the Refactoring_Config, leave the persisted Plugin_Settings unchanged, and display an error notification that names the missing setting.
7. IF the Compiler_Output_Roots list contains zero entries when the ClassTrim_Plugin is about to construct the Project_Source, THEN THE ClassTrim_Plugin SHALL not construct the Project_Source for the Analysis_Run.

### Requirement 4: Execute Analysis Off the Event-Dispatch Thread

**User Story:** As an IntelliJ developer, I want the analysis to run in the background, so that the IDE stays responsive while it runs.

#### Acceptance Criteria

1. WHEN an Analysis_Run starts, THE ClassTrim_Plugin SHALL invoke `ClassTrim_Service.analyze` off the IntelliJ event-dispatch thread on a background task scheduled through the IntelliJ ProgressManager.
2. WHILE an Analysis_Run is executing, THE ClassTrim_Plugin SHALL display a cancellable progress indicator titled "ClassTrim Analysis" in the IDE status bar.
3. WHEN the developer cancels the progress indicator, THE ClassTrim_Plugin SHALL request cancellation of the Analysis_Run within 1 second of the developer's cancel action.
4. WHEN an Analysis_Run is cancelled, THE ClassTrim_Plugin SHALL not publish any Move_Method_Suggestion produced during the cancelled Analysis_Run.
5. THE ClassTrim_Plugin SHALL allow at most one concurrent Analysis_Run per IDE_Project.
6. WHEN an Analysis_Run terminates by completion, failure, or cancellation, THE ClassTrim_Plugin SHALL remove its progress indicator from the IDE status bar within 1 second of termination.
7. IF the developer triggers a new Analysis_Run for an IDE_Project while another Analysis_Run is already executing for the same IDE_Project, THEN THE ClassTrim_Plugin SHALL reject the new Analysis_Run, leave the executing Analysis_Run unaffected, and display a notification indicating that an Analysis_Run is already in progress.

### Requirement 5: Surface Analysis Results to the Developer

**User Story:** As an IntelliJ developer, I want to know when analysis finishes and how many suggestions were produced, so that I can decide what to do next.

#### Acceptance Criteria

1. WHEN an Analysis_Run completes successfully, THE ClassTrim_Plugin SHALL publish the resulting list of Move_Method_Suggestion entries to the Tool_Window for the IDE_Project within 2 seconds of completion, replacing any previously published list of Move_Method_Suggestion entries for that IDE_Project.
2. WHEN an Analysis_Run completes successfully and produces one or more Move_Method_Suggestion entries, THE ClassTrim_Plugin SHALL display an information-severity notification within 2 seconds of completion whose body reports the total count of Move_Method_Suggestion entries as a non-negative integer.
3. WHEN an Analysis_Run completes successfully and produces zero Move_Method_Suggestion entries, THE ClassTrim_Plugin SHALL display an information-severity notification within 2 seconds of completion whose body indicates that no Move_Method_Suggestion entries were produced.
4. IF an Analysis_Run terminates without success due to failure or cancellation, THEN THE ClassTrim_Plugin SHALL display an error-severity notification within 2 seconds of termination whose body indicates that the analysis did not complete.
5. IF an Analysis_Run terminates without success due to failure or cancellation, THEN THE ClassTrim_Plugin SHALL leave any previously published list of Move_Method_Suggestion entries in the Tool_Window for the IDE_Project unchanged.

### Requirement 6: Handle Failures During Analysis

**User Story:** As an IntelliJ developer, I want clear feedback when an analysis fails, so that I can diagnose configuration or environment issues.

#### Acceptance Criteria

1. IF the IDE_Project reports no base path when the developer triggers the action, THEN THE ClassTrim_Plugin SHALL terminate the Analysis_Run before any class is analyzed, release any resources allocated for the Analysis_Run, and display a warning notification through the "ClassTrim Notifications" group within 1 second of termination asking the developer to open a Java project.
2. IF `ClassTrim_Service.analyze` throws an exception during an Analysis_Run, THEN THE ClassTrim_Plugin SHALL terminate the Analysis_Run, release any resources allocated for the Analysis_Run, discard any partial Move_Method_Suggestion entries produced before the exception, and log the exception with its stack trace to the IDE log under the plugin's logger within 1 second of termination.
3. IF `ClassTrim_Service.analyze` throws an exception during an Analysis_Run, THEN THE ClassTrim_Plugin SHALL display an error notification through the "ClassTrim Notifications" group within 2 seconds of termination whose body contains the fully qualified exception class name and the exception message truncated to at most 500 characters.
4. IF the Analysis_Run is cancelled by the developer, THEN THE ClassTrim_Plugin SHALL terminate the Analysis_Run, release any resources allocated for the Analysis_Run, discard any partial Move_Method_Suggestion entries produced before cancellation, and display an information notification through the "ClassTrim Notifications" group within 1 second of termination stating that the analysis was cancelled.
