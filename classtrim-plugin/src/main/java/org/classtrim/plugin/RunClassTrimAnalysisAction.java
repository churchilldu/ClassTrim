package org.classtrim.plugin;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Tools-menu and Project-view action that triggers a ClassTrim Analysis_Run
 * for the open {@link Project}, optionally scoped to a single {@link Module}.
 *
 * <p>Two invocation paths share this action:</p>
 * <ul>
 *   <li><strong>Project-wide</strong> — Tools → Run ClassTrim Analysis. The
 *       data context carries no module, so the coordinator resolves every
 *       module's compiler output as before.</li>
 *   <li><strong>Module-scoped</strong> — right-click on a module (or any
 *       file/folder inside one) in the Project view → Run ClassTrim
 *       Analysis. The data context carries the module, and the coordinator's
 *       module-aware overload resolves only that module's compiler output.</li>
 * </ul>
 *
 * <p>Module resolution falls back through three IntelliJ data keys so the
 * action picks up a module regardless of whether the click landed on a
 * module node or a nested file:</p>
 * <ol>
 *   <li>{@link LangDataKeys#MODULE_CONTEXT} — populated when the user
 *       right-clicks a module node directly.</li>
 *   <li>{@link LangDataKeys#MODULE} — the active module derived by the
 *       platform from the editor or selected element.</li>
 *   <li>{@link ModuleUtilCore#findModuleForFile(VirtualFile, Project)} on
 *       the selected file — the fallback used when the platform did not
 *       compute the module for the click target.</li>
 * </ol>
 *
 * <h2>Enablement (R1.2, R1.3)</h2>
 * The action is disabled when no project is open (R1.2) or while an
 * {@link AnalysisCoordinator} run is in flight (R1.3); module-scoped
 * presentation text ("Run ClassTrim Analysis on '<module>'") only takes
 * effect when a module is resolvable from the data context.
 */
public final class RunClassTrimAnalysisAction extends AnAction {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        boolean enabled = project != null
                && !AnalysisCoordinator.getInstance(project).isRunning();
        e.getPresentation().setEnabled(enabled);

        // Module-scoped presentation text. Only relabel when the data
        // context actually identifies a module so the Tools-menu entry
        // keeps its short, project-wide label.
        Module module = (project == null) ? null : resolveModule(e, project);
        if (module != null) {
            e.getPresentation().setText("Run ClassTrim Analysis on '" + module.getName() + "'");
        } else {
            e.getPresentation().setText("Run ClassTrim Analysis");
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        Module module = resolveModule(e, project);
        AnalysisCoordinator coordinator = AnalysisCoordinator.getInstance(project);
        if (module == null) {
            coordinator.requestRun();
        } else {
            coordinator.requestRun(module);
        }
    }

    /**
     * Resolves a module from the action's data context, falling back through
     * direct module keys and a per-file lookup. Returns {@code null} when
     * no module can be determined — the caller then runs the project-wide
     * pipeline.
     */
    private static @Nullable Module resolveModule(AnActionEvent e, Project project) {
        Module direct = e.getData(LangDataKeys.MODULE_CONTEXT);
        if (direct != null) return direct;

        Module fromContext = e.getData(LangDataKeys.MODULE);
        if (fromContext != null) return fromContext;

        VirtualFile file = e.getData(PlatformCoreDataKeys.VIRTUAL_FILE);
        if (file == null) return null;
        return ModuleUtilCore.findModuleForFile(file, project);
    }
}
