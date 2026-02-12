package net.momirealms.craftengine.core.plugin.context;

import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.random.RandomUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

//TODO 需要为random指定id
public class CommonParameterProvider implements AdditionalParameterProvider {
    private double lastRandom = -1;

    private static final Map<ContextKey<?>, Function<CommonParameterProvider, Object>> CONTEXT_FUNCTIONS = new HashMap<>();
    static {
        CONTEXT_FUNCTIONS.put(DirectContextParameters.RANDOM, (f) -> {
            f.lastRandom = RandomUtils.generateRandomDouble(0,1);
            return f.lastRandom;
        });
        CONTEXT_FUNCTIONS.put(DirectContextParameters.LAST_RANDOM, (f) -> {
            if (f.lastRandom == -1) {
                f.lastRandom = RandomUtils.generateRandomDouble(0, 1);
            }
            return f.lastRandom;
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> getOptionalParameter(ContextKey<T> parameter) {
        return (Optional<T>) Optional.ofNullable(CONTEXT_FUNCTIONS.get(parameter)).map(f -> f.apply(this));
    }
}
