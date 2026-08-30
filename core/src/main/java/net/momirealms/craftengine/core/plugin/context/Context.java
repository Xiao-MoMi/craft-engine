package net.momirealms.craftengine.core.plugin.context;

import net.kyori.adventure.pointer.Pointered;
import net.momirealms.craftengine.core.util.random.RandomSource;
import net.momirealms.craftengine.core.util.random.ThreadLocalRandomSource;

import java.util.Map;
import java.util.Optional;

public interface Context extends Pointered {

    ContextHolder contexts();

    <T> Optional<T> getOptionalParameter(ContextKey<T> parameter);

    default <T> T getParameterOrThrow(ContextKey<T> parameter) {
        return getOptionalParameter(parameter).orElseThrow(() -> new RuntimeException("No parameter found for " + parameter));
    }

    default RandomSource random() {
        return ThreadLocalRandomSource.INSTANCE;
    }

    void setVariable(String key, Object value);

    Object getVariable(String key);

    Map<String, Object> variables();
}
