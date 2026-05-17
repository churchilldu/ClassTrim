package org.classtrim.plugin;

import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;

import javax.swing.SwingUtilities;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * A dedicated "ClassTrim Log" console tab inside the ClassTrim tool window.
 * When debug logging is enabled in settings, all {@code [ClassTrim DEBUG]}
 * messages are streamed here in real time — no need to open idea.log or
 * configure the IDE's Debug Log Settings.
 *
 * <p>Usage from anywhere in the plugin:</p>
 * <pre>
 *   ClassTrimConsole.getInstance(project).log("Resolved roots: " + roots);
 *   ClassTrimConsole.getInstance(project).error("Analysis failed: " + msg);
 * </pre>
 *
 * <p>The console tab is lazily created on the first {@link #log} call and
 * lives as a second content tab inside the existing "ClassTrim" tool window
 * (alongside the suggestions table). If the tool window hasn't been opened
 * yet, the tab is deferred until it becomes available.</p>
 */
@Service(Service.Level.PROJECT)
public final class ClassTrimConsole {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final Project project;
    private volatile ConsoleView consoleView;
    private volatile boolean initialized;

    public ClassTrimConsole(Project project) {
        this.project = project;
    }

    public static ClassTrimConsole getInstance(Project project) {
        return project.getService(ClassTrimConsole.class);
    }

    /**
     * Logs an informational message to the ClassTrim Log tab.
     * Only outputs when debug is enabled in settings.
     */
    public void log(String message) {
        if (!isDebugEnabled()) return;
        print("[" + timestamp() + "] " + message + "\n", ConsoleViewContentType.NORMAL_OUTPUT);
    }

    /**
     * Logs an error message to the ClassTrim Log tab (always, regardless of debug flag).
     */
    public void error(String message) {
        print("[" + timestamp() + "] ERROR: " + message + "\n", ConsoleViewContentType.ERROR_OUTPUT);
    }

    /**
     * Clears the console content.
     */
    public void clear() {
        if (consoleView != null) {
            SwingUtilities.invokeLater(() -> consoleView.clear());
        }
    }

    private boolean isDebugEnabled() {
        return org.classtrim.plugin.settings.ClassTrimSettingsState.getInstance(project).isDebugEnabled();
    }

    private void print(String text, ConsoleViewContentType type) {
        ensureInitialized();
        if (consoleView != null) {
            consoleView.print(text, type);
        }
    }

    private void ensureInitialized() {
        if (initialized) return;
        synchronized (this) {
            if (initialized) return;
            SwingUtilities.invokeLater(this::createConsoleTab);
            initialized = true;
        }
    }

    private void createConsoleTab() {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("ClassTrim");
        if (toolWindow == null) return;

        ConsoleView console = TextConsoleBuilderFactory.getInstance()
                .createBuilder(project)
                .getConsole();
        this.consoleView = console;

        ContentManager cm = toolWindow.getContentManager();
        Content content = cm.getFactory().createContent(console.getComponent(), "Log", false);
        content.setDisposer(console);
        cm.addContent(content);
    }

    private static String timestamp() {
        return LocalTime.now().format(TIME_FMT);
    }
}
