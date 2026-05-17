package org.classtrim.plugin.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.FormBuilder;
import org.classtrim.plugin.SettingsView;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

/**
 * Settings UI page for the per-project ClassTrim configuration.
 *
 * <p>Renders five spinners — three metric thresholds (WMC, CBO, RFC) plus the
 * NSGA-III population size and maximum-iteration count — and persists edits
 * through {@link ClassTrimSettingsState#updateFrom(int, int, int, int, int)}.
 *
 * <p>The {@code populationSize} and {@code maxIterations} spinners use a
 * {@link SpinnerNumberModel} with {@code minimum = 1}, enforcing the R3.5
 * minimum-value contract at the UI level. The runtime
 * {@code AnalysisRunFactory.validate(...)} keeps that contract enforceable for
 * any code path that bypasses the UI (for example, manual edits to
 * {@code classtrim.xml}).
 *
 * <p>{@link #reset()} and {@link #isModified()} both read through
 * {@link ClassTrimSettingsState#view()} so the UI sees a consistent immutable
 * snapshot of the persisted state. Spinner values are coerced through
 * {@link Number#intValue()} so the configurable is robust to spinner-model
 * implementations that return non-{@link Integer} numeric types.
 *
 * <p>Backs Requirement 3.1 (read settings from the per-project service before
 * any analysis run) and Requirement 3.5 (population size and max iterations
 * have a minimum of 1).
 */
public final class ClassTrimSettingsConfigurable implements Configurable {
    private final Project project;
    private JPanel panel;
    private JSpinner wmcSpinner;
    private JSpinner cboSpinner;
    private JSpinner rfcSpinner;
    private JSpinner populationSpinner;
    private JSpinner iterationsSpinner;
    private JCheckBox debugCheckbox;
    private JCheckBox guidingObjectivesCheckbox;

    public ClassTrimSettingsConfigurable(Project project) {
        this.project = project;
    }

    @Override
    public @Nls String getDisplayName() {
        return "ClassTrim";
    }

    @Override
    public @Nullable JComponent createComponent() {
        SettingsView view = ClassTrimSettingsState.getInstance(project).view();

        // wmc/cbo/rfc: requirements impose no minimum; clamp at 0 to avoid negatives.
        wmcSpinner = new JSpinner(new SpinnerNumberModel(view.wmc(), 0, Integer.MAX_VALUE, 1));
        cboSpinner = new JSpinner(new SpinnerNumberModel(view.cbo(), 0, Integer.MAX_VALUE, 1));
        rfcSpinner = new JSpinner(new SpinnerNumberModel(view.rfc(), 0, Integer.MAX_VALUE, 1));

        // populationSize / maxIterations: min=1 enforces R3.5 at the UI level.
        populationSpinner = new JSpinner(
                new SpinnerNumberModel(view.populationSize(), 1, Integer.MAX_VALUE, 1));
        iterationsSpinner = new JSpinner(
                new SpinnerNumberModel(view.maxIterations(), 1, Integer.MAX_VALUE, 1));

        debugCheckbox = new JCheckBox("Enable debug logging");
        debugCheckbox.setSelected(ClassTrimSettingsState.getInstance(project).isDebugEnabled());

        guidingObjectivesCheckbox = new JCheckBox("Use guiding objectives (guide algorithm to right direction)");
        guidingObjectivesCheckbox.setSelected(ClassTrimSettingsState.getInstance(project).isUseGuidingObjectives());

        panel = FormBuilder.createFormBuilder()
                .addLabeledComponent("WMC threshold", wmcSpinner)
                .addLabeledComponent("CBO threshold", cboSpinner)
                .addLabeledComponent("RFC threshold", rfcSpinner)
                .addLabeledComponent("Population size", populationSpinner)
                .addLabeledComponent("Max iterations", iterationsSpinner)
                .addSeparator()
                .addComponent(guidingObjectivesCheckbox)
                .addSeparator()
                .addComponent(debugCheckbox)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();

        return panel;
    }

    @Override
    public boolean isModified() {
        if (panel == null) {
            return false;
        }
        SettingsView view = ClassTrimSettingsState.getInstance(project).view();
        ClassTrimSettingsState settings = ClassTrimSettingsState.getInstance(project);
        return view.wmc() != intValue(wmcSpinner)
                || view.cbo() != intValue(cboSpinner)
                || view.rfc() != intValue(rfcSpinner)
                || view.populationSize() != intValue(populationSpinner)
                || view.maxIterations() != intValue(iterationsSpinner)
                || settings.isDebugEnabled() != debugCheckbox.isSelected()
                || settings.isUseGuidingObjectives() != guidingObjectivesCheckbox.isSelected();
    }

    @Override
    public void apply() {
        if (panel == null) {
            return;
        }
        ClassTrimSettingsState settings = ClassTrimSettingsState.getInstance(project);
        settings.updateFrom(
                intValue(wmcSpinner),
                intValue(cboSpinner),
                intValue(rfcSpinner),
                intValue(populationSpinner),
                intValue(iterationsSpinner)
        );
        settings.setDebugEnabled(debugCheckbox.isSelected());
        settings.setUseGuidingObjectives(guidingObjectivesCheckbox.isSelected());
    }

    @Override
    public void reset() {
        if (panel == null) {
            return;
        }
        SettingsView view = ClassTrimSettingsState.getInstance(project).view();
        ClassTrimSettingsState settings = ClassTrimSettingsState.getInstance(project);
        wmcSpinner.setValue(view.wmc());
        cboSpinner.setValue(view.cbo());
        rfcSpinner.setValue(view.rfc());
        populationSpinner.setValue(view.populationSize());
        iterationsSpinner.setValue(view.maxIterations());
        debugCheckbox.setSelected(settings.isDebugEnabled());
        guidingObjectivesCheckbox.setSelected(settings.isUseGuidingObjectives());
    }

    @Override
    public void disposeUIResources() {
        panel = null;
        wmcSpinner = null;
        cboSpinner = null;
        rfcSpinner = null;
        populationSpinner = null;
        iterationsSpinner = null;
        debugCheckbox = null;
        guidingObjectivesCheckbox = null;
    }

    private static int intValue(JSpinner spinner) {
        return ((Number) spinner.getValue()).intValue();
    }
}
