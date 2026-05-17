package org.classtrim.plugin;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;

/**
 * Thin wrapper around the {@code "ClassTrim Notifications"} notification group.
 *
 * <p>Every public severity method ({@link #info(Project, String, String)},
 * {@link #warning(Project, String, String)}, {@link #error(Project, String, String)})
 * resolves the {@code "ClassTrim Notifications"} group through
 * {@link NotificationGroupManager}, builds a {@link Notification} with the matching
 * {@link NotificationType}, and dispatches it to the supplied project. Centralizing
 * notification dispatch here satisfies requirements 6.1, 6.3, and 6.4 by routing
 * every plugin message through a single, filterable channel.
 *
 * <p>Body-formatting helpers ({@link #truncate(String, int)},
 * {@link #formatFailureBody(String, String)}) are exposed as static utilities so that
 * tests and call-sites that only need to format a message — without dispatching a
 * notification — do not need to construct a notifier instance. The success-body
 * helper is added separately in task 5.3.
 */
public final class ClassTrimNotifier {

    /** Notification group id registered in {@code plugin.xml}. */
    static final String GROUP_ID = "ClassTrim Notifications";

    /**
     * Maximum length of the message portion of a failure notification body, in
     * {@code char}s. Mirrors the cap declared in requirement 6.3.
     */
    public static final int FAILURE_MESSAGE_MAX_LENGTH = 500;

    private static final ClassTrimNotifier INSTANCE = new ClassTrimNotifier();

    /**
     * Returns the shared notifier. The notifier is stateless, so a single instance
     * is reused across the plugin.
     */
    public static ClassTrimNotifier getInstance() {
        return INSTANCE;
    }

    /** Public no-arg constructor so the notifier can also be instantiated directly. */
    public ClassTrimNotifier() {
    }

    /** Dispatch an information-severity notification through the ClassTrim group. */
    public void info(Project project, String title, String body) {
        notify(project, title, body, NotificationType.INFORMATION);
    }

    /** Dispatch a warning-severity notification through the ClassTrim group. */
    public void warning(Project project, String title, String body) {
        notify(project, title, body, NotificationType.WARNING);
    }

    /** Dispatch an error-severity notification through the ClassTrim group. */
    public void error(Project project, String title, String body) {
        notify(project, title, body, NotificationType.ERROR);
    }

    /**
     * Truncates {@code s} to at most {@code max} characters.
     *
     * <p>Behaviour (per task 5.2 in the {@code idea-plugin} spec):
     * <ul>
     *   <li>{@code s == null}             → returns {@code ""}.</li>
     *   <li>{@code s.length() <= max}     → returns {@code s} (unchanged reference).</li>
     *   <li>{@code s.length() >  max}     → returns {@code s.substring(0, max)}.</li>
     * </ul>
     *
     * <p>This helper never throws on any input (including {@code null}, very long
     * strings, embedded newlines, or non-ASCII characters), so it is safe to call
     * directly on an exception message.
     *
     * @param s   the string to truncate; may be {@code null}.
     * @param max the maximum number of characters to retain.
     * @return the truncated string, never {@code null}.
     */
    public static String truncate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max);
    }

    /**
     * Formats the body of a failure notification as
     * {@code className + ": " + truncate(messageOrNull, 500)}.
     *
     * <p>This is the body required by requirement 6.3 and design Property 7:
     * the fully qualified exception class name, the {@code ": "} separator, and
     * the exception message capped at {@value #FAILURE_MESSAGE_MAX_LENGTH}
     * characters. A {@code null} {@code messageOrNull} contributes the empty
     * string after the separator (per the {@link #truncate(String, int)}
     * contract). The helper never throws on any input.
     *
     * @param className     the exception class name.
     * @param messageOrNull the exception message; may be {@code null} or arbitrarily long.
     * @return the formatted failure-notification body, never {@code null}.
     */
    public static String formatFailureBody(String className, String messageOrNull) {
        return className + ": " + truncate(messageOrNull, FAILURE_MESSAGE_MAX_LENGTH);
    }

    /**
     * Formats the body of a success notification reporting the number of
     * move-method suggestions produced by an analysis run.
     *
     * <p>Behaviour (per task 5.3 in the {@code idea-plugin} spec and design
     * Property 6):
     * <ul>
     *   <li>The body contains the decimal representation of {@code suggestionCount}
     *       such that parsing the count out of the body returns the input value.</li>
     *   <li>The body contains exactly one decimal integer literal — the count
     *       itself — so a regex like {@code \\d+} extracts {@code suggestionCount}
     *       unambiguously.</li>
     *   <li>When {@code suggestionCount == 0}, the body additionally includes a
     *       phrase indicating that no move-method suggestions were produced
     *       (requirement 5.3).</li>
     * </ul>
     *
     * @param suggestionCount the non-negative count of move-method suggestions.
     * @return the formatted success-notification body, never {@code null}.
     */
    public static String formatSuccessBody(int suggestionCount) {
        if (suggestionCount == 0) {
            return "No move-method suggestions were produced (count: 0)";
        }
        return "Generated " + suggestionCount + " move-method suggestions";
    }

    private void notify(Project project, String title, String body, NotificationType type) {
        NotificationGroup group = NotificationGroupManager.getInstance().getNotificationGroup(GROUP_ID);
        Notification notification = group.createNotification(
                title == null ? "" : title,
                body == null ? "" : body,
                type
        );
        notification.notify(project);
    }
}
