package net.momirealms.craftengine.core.plugin.context.parameter;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.context.ChainParameterProvider;
import net.momirealms.craftengine.core.plugin.context.ContextKey;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class ItemParameterProvider implements ChainParameterProvider<Item<?>> {
    private static final Map<ContextKey<?>, Function<Item<?>, Object>> CONTEXT_FUNCTIONS = new HashMap<>();
    static {
        CONTEXT_FUNCTIONS.put(DirectContextParameters.ID, Item::id);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.CUSTOM_MODEL_DATA, i -> i.customModelData().orElse(null));
        CONTEXT_FUNCTIONS.put(DirectContextParameters.IS_CUSTOM, Item::isCustomItem);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> getOptionalParameter(ContextKey<T> parameter, Item<?> world) {
        return (Optional<T>) Optional.ofNullable(CONTEXT_FUNCTIONS.get(parameter)).map(f -> f.apply(world));
    }
}