package org.classtrim.plugin.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.FormBuilder;
import org.classtrim.plugin.SettingsView;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

public final class ClassTrimSettingsConfigurable implements Configurable {
    private final Project project;
    private JPanel panel;
    private JSpinner wmcSpinner;
    private JSpinner cboSpinner;
    private JSpinner rfcSpinner;
    private JSpinner populationSpinner;
    private JSpinner iterationsSpinner;
    private JCheckBox guidingObjectivesCheckbox;
    private JCheckBox debugCheckbox;
    private JComboBox<String> algorithmCombo;

    public ClassTrimSettingsConfigurable(Project project) {
        this.project = project;
    }

    @Override
    public @Nls String getDisplayName() {
        return "ClassTrim";
    }

    @Override
    public @Nullable JComponent createComponent() {
        ClassTrimSettingsState settings = ClassTrimSettingsState.getInstance(project);
        SettingsView view = settings.view();

        wmcSpinner = new JSpinner(new SpinnerNumberModel(view.wmc(), 0, Integer.MAX_VALUE, 1));
        cboSpinner = new JSpinner(new SpinnerNumberModel(view.cbo(), 0, Integer.MAX_VALUE, 1));
        rfcSpinner = new JSpinner(new SpinnerNumberModel(view.rfc(), 0, Integer.MAX_VALUE, 1));
        populationSpinner = new JSpinner(new SpinnerNumberModel(view.populationSize(), 1, Integer.MAX_VALUE, 1));
        iterationsSpinner = new JSpinner(new SpinnerNumberModel(view.maxIterations(), 1, Integer.MAX_VALUE, 1));

        guidingObjectivesCheckbox = new JCheckBox("Use guiding objectives (6 objectives instead of 3)");
        guidingObjectivesCheckbox.setSelected(settings.isUseGuidingObjectives());

        algorithmCombo = new JComboBox<>(java.util.Arrays.stream(
                org.classtrim.core.engine.AlgorithmType.values())
                .map(org.classtrim.core.engine.AlgorithmType::getDisplayName)
                .toArray(String[]::new));
        algorithmCombo.setSelectedItem(settings.getAlgorithm());

        debugCheckbox = new JCheckBox("Enable debug logging");
        debugCheckbox.setSelected(settings.isDebugEnabled());

        panel = FormBuilder.createFormBuilder()
                .addLabeledComponent("WMC threshold", wmcSpinner)
                .addLabeledComponent("CBO threshold", cboSpinner)
                .addLabeledComponent("RFC threshold", rfcSpinner)
                .addLabeledComponent("Population size", populationSpinner)
                .addLabeledComponent("Max iterations", iterationsSpinner)
                .addLabeledComponent("Algorithm", algorithmCombo)
                .addSeparator()
                .addComponent(guidingObjectivesCheckbox)
                .addComponent(debugCheckbox)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();

        return panel;
    }

    @Override
    public boolean isModified() {
        if (panel == null) return false;
        ClassTrimSettingsState settings = ClassTrimSettingsState.getInstance(project);
        SettingsView view = settings.view();
        return view.wmc() != intValue(wmcSpinner)
                || view.cbo() != intValue(cboSpinner)
                || view.rfc() != intValue(rfcSpinner)
                || view.populationSize() != intValue(populationSpinner)
                || view.maxIterations() != intValue(iterationsSpinner)
                || settings.isUseGuidingObjectives() != guidingObjectivesCheckbox.isSelected()
                || !settings.getAlgorithm().equals(algorithmCombo.getSelectedItem())
                || settings.isDebugEnabled() != debugCheckbox.isSelected();
    }

    @Override
    public void apply() {
        if (panel == null) return;
        ClassTrimSettingsState settings = ClassTrimSettingsState.getInstance(project);
        settings.updateFrom(
                intValue(wmcSpinner),
                intValue(cboSpinner),
                intValue(rfcSpinner),
                intValue(populationSpinner),
                intValue(iterationsSpinner)
        );
        settings.setUseGuidingObjectives(guidingObjectivesCheckbox.isSelected());
        settings.setAlgorithm((String) algorithmCombo.getSelectedItem());
        settings.setDebugEnabled(debugCheckbox.isSelected());
    }

    @Override
    public void reset() {
        if (panel == null) return;
        ClassTrimSettingsState settings = ClassTrimSettingsState.getInstance(project);
        SettingsView view = settings.view();
        wmcSpinner.setValue(view.wmc());
        cboSpinner.setValue(view.cbo());
        rfcSpinner.setValue(view.rfc());
        populationSpinner.setValue(view.populationSize());
        iterationsSpinner.setValue(view.maxIterations());
        guidingObjectivesCheckbox.setSelected(settings.isUseGuidingObjectives());
        algorithmCombo.setSelectedItem(settings.getAlgorithm());
        debugCheckbox.setSelected(settings.isDebugEnabled());
    }

    @Override
    public void disposeUIResources() {
        panel = null;
        wmcSpinner = null;
        cboSpinner = null;
        rfcSpinner = null;
        populationSpinner = null;
        iterationsSpinner = null;
        guidingObjectivesCheckbox = null;
        algorithmCombo = null;
        debugCheckbox = null;
    }

    private static int intValue(JSpinner spinner) {
        return ((Number) spinner.getValue()).intValue();
    }
}
