package net.momirealms.craftengine.core.plugin.context;

import net.momirealms.craftengine.core.plugin.context.parameter.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractChainParameterContext extends AbstractCommonContext {
    private static final Map<ContextKey<?>, ChainParameterProvider<?>> CHAIN_PARAMETERS = new HashMap<>();
    static {
        CHAIN_PARAMETERS.put(DirectContextParameters.PLAYER, new PlayerParameterProvider());
        CHAIN_PARAMETERS.put(DirectContextParameters.WORLD, new WorldParameterProvider());
        CHAIN_PARAMETERS.put(DirectContextParameters.BLOCK, new BlockParameterProvider());
        CHAIN_PARAMETERS.put(DirectContextParameters.POSITION, new PositionParameterProvider());
        CHAIN_PARAMETERS.put(DirectContextParameters.FURNITURE, new FurnitureParameterProvider());
        CHAIN_PARAMETERS.put(DirectContextParameters.ENTITY, new EntityParameterProvider());
        ItemParameterProvider itemProvider = new ItemParameterProvider();
        CHAIN_PARAMETERS.put(DirectContextParameters.MAIN_HAND_ITEM, itemProvider);
        CHAIN_PARAMETERS.put(DirectContextParameters.OFF_HAND_ITEM, itemProvider);
        CHAIN_PARAMETERS.put(DirectContextParameters.FURNITURE_ITEM, itemProvider);
        CHAIN_PARAMETERS.put(DirectContextParameters.CONSUMED_ITEM, itemProvider);
        CHAIN_PARAMETERS.put(DirectContextParameters.ITEM_IN_HAND, itemProvider);
    }

    @SuppressWarnings("unchecked")
    private static <T> ChainParameterProvider<T> getParameterProvider(final ContextKey<?> key) {
        return (ChainParameterProvider<T>) CHAIN_PARAMETERS.get(key);
    }

    public AbstractChainParameterContext(ContextHolder contexts) {
        super(contexts);
    }

    public AbstractChainParameterContext(ContextHolder contexts,
                                         List<AdditionalParameterProvider> additionalParameterProviders) {
        super(contexts, additionalParameterProviders);
    }

    @Override
    public <T> Optional<T> getOptionalParameter(ContextKey<T> parameter) {
        ContextKey<Object> parentKey = parameter.parent();
        if (parentKey == null) {
            return super.getOptionalParameter(parameter);
        }
        if (!CHAIN_PARAMETERS.containsKey(parentKey)) {
            return Optional.empty();
        }
        Optional<Object> parentValue = getOptionalParameter(parentKey);
        if (parentValue.isEmpty()) {
            return Optional.empty();
        }
        return getParameterProvider(parentKey).getOptionalParameter(parameter, parentValue.get());
    }
}
