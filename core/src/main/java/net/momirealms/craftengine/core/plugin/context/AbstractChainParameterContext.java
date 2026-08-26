package net.momirealms.craftengine.core.plugin.context;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;

import java.util.Map;
import java.util.Optional;

public abstract class AbstractChainParameterContext implements Context {
    protected final ContextHolder contexts;
    protected final Object2ObjectArrayMap<String, Object> vars = new Object2ObjectArrayMap<>();

    public AbstractChainParameterContext(ContextHolder contexts) {
        this.contexts = contexts;
    }

    @Override
    public <T> Optional<T> getOptionalParameter(ContextKey<T> parameter) {
        ContextKey<Object> parentKey = parameter.parent();
        if (parentKey == null) {
            return this.contexts.getOptional(parameter);
        }
        Optional<Object> parentValue = getOptionalParameter(parentKey);
        if (parentValue.isEmpty()) {
            return Optional.empty();
        }
        if (parentValue.get() instanceof ChainParameterSource source) {
            return source.getParameter(parameter);
        }
        return Optional.empty();
    }

    @Override
    public ContextHolder contexts() {
        return this.contexts;
    }

    @Override
    public void setVariable(String key, Object value) {
        this.vars.put(key, value);
    }

    @Override
    public Object getVariable(String key) {
        return this.vars.get(key);
    }

    @Override
    public Map<String, Object> variables() {
        return this.vars;
    }
}
