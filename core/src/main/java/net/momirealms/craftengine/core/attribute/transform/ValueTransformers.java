package net.momirealms.craftengine.core.attribute.transform;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.Map;

public final class ValueTransformers {
    public static final ValueTransformerType<ExpressionValueTransformer> EXPRESSION = register(Key.ce("expression"), ExpressionValueTransformer.FACTORY);

    private ValueTransformers() {}

    public static <T extends ValueTransformer> ValueTransformerType<T> register(Key key, ValueTransformerFactory<T> factory) {
        ValueTransformerType<T> type = new ValueTransformerType<>(key, factory);
        ((WritableRegistry<ValueTransformerType<? extends ValueTransformer>>) BuiltInRegistries.VALUE_TRANSFORMER_TYPE)
                .register(ResourceKey.create(Registries.VALUE_TRANSFORMER_TYPE.location(), key), type);
        return type;
    }

    public static ValueTransformer fromConfig(ConfigValue value) {
        if (value.is(Map.class)) {
            return fromConfig(value.getAsSection());
        }
        return new ExpressionValueTransformer(value.path(), value.getAsString());
    }

    public static ValueTransformer fromConfig(ConfigSection section) {
        String type = section.getString("type", "expression");
        Key key = Key.ce(type);
        ValueTransformerType<? extends ValueTransformer> transformerType = BuiltInRegistries.VALUE_TRANSFORMER_TYPE.getValue(key);
        if (transformerType == null) {
            throw new KnownResourceException("attribute.value_transformer.unknown_type", section.assemblePath("type"), type);
        }
        return transformerType.factory().create(section);
    }
}
