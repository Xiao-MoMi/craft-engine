package net.momirealms.craftengine.core.plugin.context.parameter;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.processor.RandomValuesProcessor;
import net.momirealms.craftengine.core.plugin.context.ChainParameterProvider;
import net.momirealms.craftengine.core.plugin.context.ContextKey;
import net.momirealms.craftengine.core.plugin.context.NamedRandoms;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;

public final class ItemParameterProvider implements ChainParameterProvider<Item> {
    public static final ItemParameterProvider INSTANCE = new ItemParameterProvider();
    private static final Map<ContextKey<?>, Function<Item, Object>> CONTEXT_FUNCTIONS = new HashMap<>();

    static {
        CONTEXT_FUNCTIONS.put(DirectContextParameters.ID, Item::id);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.CUSTOM_MODEL_DATA, i -> i.customModelData().orElse(null));
        CONTEXT_FUNCTIONS.put(DirectContextParameters.IS_CUSTOM, Item::isCustomItem);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.COUNT, Item::count);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.IS_BLOCK_ITEM, Item::isBlockItem);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.RANDOM, ItemParameterProvider::randomValues);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> getOptionalParameter(ContextKey<T> parameter, Item item) {
        return (Optional<T>) Optional.ofNullable(CONTEXT_FUNCTIONS.get(parameter)).map(f -> f.apply(item));
    }

    private static NamedRandoms randomValues(Item item) {
        if (item.getMinecraftTag(RandomValuesProcessor.TAG_PATH) == null) {
            return null;
        }
        return new NamedRandoms(new ItemRandomValuesView(item));
    }

    private static final class ItemRandomValuesView extends AbstractMap<String, Double> {
        private final Item item;
        private Map<String, Double> localValues;
        private boolean materialized;

        private ItemRandomValuesView(Item item) {
            this.item = item;
        }

        @Override
        public Double get(Object key) {
            if (!(key instanceof String id)) {
                return null;
            }
            if (this.localValues != null) {
                Double localValue = this.localValues.get(id);
                if (localValue != null) {
                    return localValue;
                }
            }
            if (this.materialized) {
                return null;
            }
            Object value = this.item.getTagAsJava(RandomValuesProcessor.TAG_PATH, id);
            return value instanceof Number number ? number.doubleValue() : null;
        }

        @Override
        public Double put(String key, Double value) {
            Double previous = get(key);
            if (this.localValues == null) {
                this.localValues = new HashMap<>();
            }
            this.localValues.put(key, value);
            return previous;
        }

        @NotNull
        @Override
        public Set<Entry<String, Double>> entrySet() {
            if (!this.materialized) {
                Map<String, Double> values = new LinkedHashMap<>();
                NamedRandoms stored = this.item.getCustomData(NamedRandoms.class, RandomValuesProcessor.TAG_PATH);
                if (stored != null) {
                    values.putAll(stored.values);
                }
                if (this.localValues != null) {
                    values.putAll(this.localValues);
                }
                this.localValues = values;
                this.materialized = true;
            }
            return this.localValues.entrySet();
        }
    }
}
