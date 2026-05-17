package org.classtrim.plugin;

import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.classtrim.plugin.settings.ClassTrimSettingsState;

/**
 * IntelliJ light test that verifies the {@code plugin.xml} extension wiring
 * registered in task 10.1 is actually picked up by the IntelliJ Platform when
 * the plugin is loaded into a test fixture.
 *
 * <p>Extends {@link BasePlatformTestCase} so that the platform's lightweight
 * in-memory project fixture is stood up — that fixture is enough to:
 * <ul>
 *   <li>resolve the {@code "ClassTrim.RunAnalysis"} action through
 *       {@link ActionManager} (R1.1);</li>
 *   <li>look up the {@code "ClassTrim"} tool-window registration through
 *       {@link ToolWindowManager} (R5.1);</li>
 *   <li>resolve the {@code "ClassTrim Notifications"} group through
 *       {@link NotificationGroupManager} (R6.1);</li>
 *   <li>inject the project-level services
 *       ({@link ClassTrimSettingsState} and {@link AnalysisCoordinator})
 *       through {@code project.getService(...)}.</li>
 * </ul>
 *
 * <p>JUnit 3-style naming ({@code public void testXxx()}) is mandatory because
 * {@link BasePlatformTestCase} ultimately extends
 * {@link junit.framework.TestCase}; the IntelliJ test framework only
 * discovers methods on {@code TestCase} subclasses through that convention.
 *
 * <p>Validates: Requirements 1.1, 5.1, 6.1.
 */
public class PluginXmlExtensionsLightTest extends BasePlatformTestCase {

    /**
     * R1.1 — the {@code "ClassTrim.RunAnalysis"} action must be registered
     * with the IntelliJ {@link ActionManager} and must resolve to an
     * instance of {@link RunClassTrimAnalysisAction}. This is the
     * configuration contract that lets the Tools-menu entry actually invoke
     * the production action class on click.
     */
    public void testActionIsRegistered() {
        AnAction action = ActionManager.getInstance().getAction("ClassTrim.RunAnalysis");

        assertNotNull(
                "R1.1: action 'ClassTrim.RunAnalysis' must be registered in plugin.xml",
                action);
        assertTrue(
                "R1.1: action 'ClassTrim.RunAnalysis' must resolve to a "
                        + RunClassTrimAnalysisAction.class.getSimpleName()
                        + " instance (was " + action.getClass().getName() + ")",
                action instanceof RunClassTrimAnalysisAction);
    }

    /**
     * R5.1 — the {@code "ClassTrim"} tool window must be registered with the
     * project-level {@link ToolWindowManager} so that successful
     * Analysis_Run results can be published into it.
     *
     * <p>{@link BasePlatformTestCase}'s lightweight project fixture does
     * <em>not</em> trigger {@code ToolWindowManager.registerToolWindow(...)}
     * for tool windows declared in {@code plugin.xml}; that registration is
     * driven by the IntelliJ IDE startup activities, which the lightweight
     * fixture does not run. Calling {@link ToolWindowManager#getToolWindow}
     * directly therefore returns {@code null} even when the extension is
     * present and correct.</p>
     *
     * <p>The robust assertion is that the {@code <toolWindow>} extension is
     * registered with the IntelliJ Platform Extension Point — i.e. the
     * platform <em>knows about</em> the registration even if the
     * lightweight fixture has not materialised it. The
     * {@code com.intellij.toolWindow} extension point exposes the registered
     * factory id; iterating its extensions and checking for {@code "ClassTrim"}
     * verifies the same wiring without depending on the test fixture
     * actually creating the tool window.</p>
     */
    public void testToolWindowFactoryIsRegistered() {
        com.intellij.openapi.extensions.ExtensionPointName<com.intellij.openapi.wm.ToolWindowEP> ep =
                com.intellij.openapi.extensions.ExtensionPointName.create("com.intellij.toolWindow");

        boolean found = false;
        for (com.intellij.openapi.wm.ToolWindowEP extension : ep.getExtensionList()) {
            if ("ClassTrim".equals(extension.id)) {
                found = true;
                assertEquals(
                        "R5.1: tool window 'ClassTrim' must be backed by ClassTrimToolWindowFactory",
                        ClassTrimToolWindowFactory.class.getName(),
                        extension.factoryClass);
                break;
            }
        }

        assertTrue(
                "R5.1: tool window 'ClassTrim' must be registered in plugin.xml so "
                        + "Analysis_Run results have a publication target",
                found);
    }

    /**
     * R6.1 — the {@code "ClassTrim Notifications"} group must be registered
     * with {@link NotificationGroupManager} so that
     * {@link ClassTrimNotifier} can dispatch precondition warnings,
     * cancellation info notifications, and failure error notifications
     * through it. Resolution failure here means the
     * {@code <notificationGroup>} extension in {@code plugin.xml} is missing
     * or mis-spelled.
     */
    public void testNotificationGroupIsRegistered() {
        NotificationGroup group = NotificationGroupManager.getInstance()
                .getNotificationGroup("ClassTrim Notifications");

        assertNotNull(
                "R6.1: notification group 'ClassTrim Notifications' must be registered "
                        + "in plugin.xml",
                group);
    }

    /**
     * The project-level services declared by the plugin's
     * {@code <projectService>} extensions must be injectable through
     * {@code project.getService(...)}. Concretely:
     * <ul>
     *   <li>{@link ClassTrimSettingsState#getInstance(Project)} must return a
     *       non-{@code null} settings service;</li>
     *   <li>{@link AnalysisCoordinator#getInstance(Project)} must return a
     *       non-{@code null} coordinator service.</li>
     * </ul>
     * If either lookup returns {@code null}, the corresponding
     * {@code <projectService>} entry is missing from {@code plugin.xml} and
     * the plugin's runtime wiring is broken.
     */
    public void testProjectServicesAreInjectable() {
        Project project = getProject();

        ClassTrimSettingsState settings = ClassTrimSettingsState.getInstance(project);
        assertNotNull(
                "Project service 'ClassTrimSettingsState' must be registered in plugin.xml",
                settings);

        AnalysisCoordinator coordinator = AnalysisCoordinator.getInstance(project);
        assertNotNull(
                "Project service 'AnalysisCoordinator' must be registered in plugin.xml",
                coordinator);
    }
}
