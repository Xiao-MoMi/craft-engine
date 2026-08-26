package net.momirealms.craftengine.core.plugin.context;

import java.util.Map;
import java.util.Optional;

public class ViewerContext implements RelationalContext {
    private final Context owner;
    private final PlayerOptionalContext viewer;

    public ViewerContext(Context owner, PlayerOptionalContext viewer) {
        this.owner = owner;
        this.viewer = viewer;
    }

    public static ViewerContext of(Context owner, PlayerOptionalContext viewer) {
        return new ViewerContext(owner, viewer);
    }

    public Context owner() {
        return this.owner;
    }

    public PlayerOptionalContext viewer() {
        return this.viewer;
    }

    @Override
    public <T> Optional<T> getViewerOptionalParameter(ContextKey<T> parameter) {
        return this.viewer.getOptionalParameter(parameter);
    }

    @Override
    public ContextHolder viewerContexts() {
        return this.viewer.contexts();
    }

    @Override
    public <T> T getViewerParameterOrThrow(ContextKey<T> parameter) {
        return this.viewer.getParameterOrThrow(parameter);
    }

    @Override
    public ContextHolder contexts() {
        return this.owner.contexts();
    }

    @Override
    public <T> Optional<T> getOptionalParameter(ContextKey<T> parameter) {
        return this.owner.getOptionalParameter(parameter);
    }

    @Override
    public <T> T getParameterOrThrow(ContextKey<T> parameter) {
        return this.owner.getParameterOrThrow(parameter);
    }

    @Override
    public void setVariable(String key, Object value) {
        this.owner.setVariable(key, value);
    }

    @Override
    public Object getVariable(String key) {
        return this.owner.getVariable(key);
    }

    @Override
    public Map<String, Object> variables() {
        return this.owner.variables();
    }
}
