package org.classtrim.plugin.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.classtrim.common.Threshold;
import org.classtrim.plugin.SettingsView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(name = "ClassTrimSettings", storages = @Storage("classtrim.xml"))
@Service(Service.Level.PROJECT)
public final class ClassTrimSettingsState implements PersistentStateComponent<ClassTrimSettingsState.State> {

    /**
     * Persisted, mutable settings shape. Field defaults must mirror
     * {@link Defaults#DEFAULTS}; both are the single source of truth for
     * R3.4 and are referenced by validation per R3.6.
     */
    public static final class State {
        public int wmc = Defaults.DEFAULTS.wmc();
        public int cbo = Defaults.DEFAULTS.cbo();
        public int rfc = Defaults.DEFAULTS.rfc();
        public int populationSize = Defaults.DEFAULTS.populationSize();
        public int maxIterations = Defaults.DEFAULTS.maxIterations();
        public boolean debugEnabled = false;
        public boolean useGuidingObjectives = true;
        public String algorithm = "NSGA-III";
    }

    /**
     * Immutable carrier of the design-mandated default values for every
     * persisted Plugin_Settings field. Validation consults
     * {@link #defaults()} to satisfy R3.4 (use the declared default when no
     * developer-assigned value is present) and R3.6 (surface a missing-default
     * error when a default cannot be retrieved).
     */
    public record Defaults(int wmc, int cbo, int rfc, int populationSize, int maxIterations) {
        static final Defaults DEFAULTS = new Defaults(8, 8, 30, 500, 2000);
    }

    private State state = new State();

    public static ClassTrimSettingsState getInstance(Project project) {
        return project.getService(ClassTrimSettingsState.class);
    }

    /**
     * Returns the design-mandated default values for the persisted settings.
     * The returned record is immutable and never {@code null}.
     */
    public static Defaults defaults() {
        return Defaults.DEFAULTS;
    }

    @Override
    public @Nullable State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }

    public Threshold toThreshold() {
        return new Threshold(state.wmc, state.cbo, state.rfc);
    }

    public int getPopulationSize() {
        return state.populationSize;
    }

    public int getMaxIterations() {
        return state.maxIterations;
    }

    /**
     * Returns an immutable snapshot of the current persisted settings.
     * The returned {@link SettingsView} captures values by copy, so subsequent
     * mutations through {@link #updateFrom(int, int, int, int, int)} or
     * {@link #loadState(State)} do not affect a previously returned view.
     * This is the read path used by validation; validation never mutates
     * the persisted state (R3.5, R3.6).
     */
    public SettingsView view() {
        return new SettingsView(
                state.wmc,
                state.cbo,
                state.rfc,
                state.populationSize,
                state.maxIterations
        );
    }

    public void updateFrom(int wmc, int cbo, int rfc, int populationSize, int maxIterations) {
        state.wmc = wmc;
        state.cbo = cbo;
        state.rfc = rfc;
        state.populationSize = populationSize;
        state.maxIterations = maxIterations;
    }

    public boolean isDebugEnabled() {
        return state.debugEnabled;
    }

    public void setDebugEnabled(boolean enabled) {
        state.debugEnabled = enabled;
    }

    public boolean isUseGuidingObjectives() {
        return state.useGuidingObjectives;
    }

    public void setUseGuidingObjectives(boolean use) {
        state.useGuidingObjectives = use;
    }

    public String getAlgorithm() {
        return state.algorithm;
    }

    public org.classtrim.core.engine.AlgorithmType getAlgorithmType() {
        return org.classtrim.core.engine.AlgorithmType.fromDisplayName(state.algorithm);
    }

    public void setAlgorithm(String algorithm) {
        state.algorithm = algorithm;
    }
}
