package org.classtrim.plugin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import org.jetbrains.annotations.NotNull;

/**
 * Tool-window factory for the "ClassTrim" tool window.
 *
 * <p>Constructs a {@link ClassTrimToolWindowPanel} (which registers itself in the
 * static {@code PANELS} map keyed by {@code project.getLocationHash()}) and
 * attaches it as the tool window's content. The actual tool-window registration
 * with id {@code "ClassTrim"} lives in {@code plugin.xml}; this factory only
 * supplies the panel.
 *
 * <p>Satisfies requirement R5.1 (results surfaced through a project tool window).
 */
public class ClassTrimToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ClassTrimToolWindowPanel panel = new ClassTrimToolWindowPanel(project);
        ContentManager cm = toolWindow.getContentManager();
        Content content = ContentFactory.getInstance().createContent(panel, "", false);
        cm.addContent(content);
    }
}
