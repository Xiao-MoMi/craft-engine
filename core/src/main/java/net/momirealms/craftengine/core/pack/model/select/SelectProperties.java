package net.momirealms.craftengine.core.pack.model.select;

import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.Map;

public class SelectProperties {
    public static final Key BLOCK_STATE = Key.of("minecraft:block_state");
    public static final Key CHARGE_TYPE = Key.of("minecraft:charge_type");
    public static final Key CONTEXT_DIMENSION = Key.of("minecraft:context_dimension");
    public static final Key CONTEXT_ENTITY_TYPE = Key.of("minecraft:context_entity_type");
    public static final Key CUSTOM_MODEL_DATA = Key.of("minecraft:custom_model_data");
    public static final Key DISPLAY_CONTEXT = Key.of("minecraft:display_context");
    public static final Key LOCAL_TIME = Key.of("minecraft:local_time");
    public static final Key MAIN_HAND = Key.of("minecraft:main_hand");
    public static final Key TRIM_MATERIAL = Key.of("minecraft:trim_material");

    static {
        register(CHARGE_TYPE, ChargeTypeSelectProperty.FACTORY);
        register(CONTEXT_DIMENSION, SimpleSelectProperty.FACTORY);
        register(CONTEXT_ENTITY_TYPE, SimpleSelectProperty.FACTORY);
        register(DISPLAY_CONTEXT, SimpleSelectProperty.FACTORY);
        register(MAIN_HAND, MainHandSelectProperty.FACTORY);
        register(TRIM_MATERIAL, TrimMaterialSelectProperty.FACTORY);
        register(BLOCK_STATE, BlockStateSelectProperty.FACTORY);
        register(CUSTOM_MODEL_DATA, CustomModelDataSelectProperty.FACTORY);
        register(LOCAL_TIME, LocalTimeSelectProperty.FACTORY);
    }

    public static void register(Key key, SelectPropertyFactory factory) {
        Holder.Reference<SelectPropertyFactory> holder = ((WritableRegistry<SelectPropertyFactory>) BuiltInRegistries.SELECT_PROPERTY_FACTORY)
                .registerForHolder(new ResourceKey<>(Registries.SELECT_PROPERTY_FACTORY.location(), key));
        holder.bindValue(factory);
    }

    public static SelectProperty fromMap(Map<String, Object> map) {
        String type = (String) map.get("property");
        if (type == null) {
            throw new NullPointerException("property type cannot be null");
        }
        Key key = Key.withDefaultNamespace(type, "minecraft");
        SelectPropertyFactory factory = BuiltInRegistries.SELECT_PROPERTY_FACTORY.getValue(key);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown property type: " + type);
        }
        return factory.create(map);
    }
}
