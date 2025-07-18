package net.momirealms.craftengine.core.pack.model.select;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
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
    public static final Key COMPONENT = Key.of("minecraft:component");

    static {
        registerFactory(CHARGE_TYPE, ChargeTypeSelectProperty.FACTORY);
        registerReader(CHARGE_TYPE, ChargeTypeSelectProperty.READER);
        registerFactory(CONTEXT_DIMENSION, SimpleSelectProperty.FACTORY);
        registerReader(CONTEXT_DIMENSION, SimpleSelectProperty.READER);
        registerFactory(CONTEXT_ENTITY_TYPE, SimpleSelectProperty.FACTORY);
        registerReader(CONTEXT_ENTITY_TYPE, SimpleSelectProperty.READER);
        registerFactory(DISPLAY_CONTEXT, SimpleSelectProperty.FACTORY);
        registerReader(DISPLAY_CONTEXT, SimpleSelectProperty.READER);
        registerFactory(MAIN_HAND, MainHandSelectProperty.FACTORY);
        registerReader(MAIN_HAND, MainHandSelectProperty.READER);
        registerFactory(TRIM_MATERIAL, TrimMaterialSelectProperty.FACTORY);
        registerReader(TRIM_MATERIAL, TrimMaterialSelectProperty.READER);
        registerFactory(BLOCK_STATE, BlockStateSelectProperty.FACTORY);
        registerReader(BLOCK_STATE, BlockStateSelectProperty.READER);
        registerFactory(CUSTOM_MODEL_DATA, CustomModelDataSelectProperty.FACTORY);
        registerReader(CUSTOM_MODEL_DATA, CustomModelDataSelectProperty.READER);
        registerFactory(LOCAL_TIME, LocalTimeSelectProperty.FACTORY);
        registerReader(LOCAL_TIME, LocalTimeSelectProperty.READER);
        registerFactory(COMPONENT, ComponentSelectProperty.FACTORY);
        registerReader(COMPONENT, ComponentSelectProperty.READER);
    }

    public static void registerFactory(Key key, SelectPropertyFactory factory) {
        ((WritableRegistry<SelectPropertyFactory>) BuiltInRegistries.SELECT_PROPERTY_FACTORY)
                .register(ResourceKey.create(Registries.SELECT_PROPERTY_FACTORY.location(), key), factory);
    }

    public static void registerReader(Key key, SelectPropertyReader reader) {
        ((WritableRegistry<SelectPropertyReader>) BuiltInRegistries.SELECT_PROPERTY_READER)
                .register(ResourceKey.create(Registries.SELECT_PROPERTY_READER.location(), key), reader);
    }

    public static SelectProperty fromMap(Map<String, Object> map) {
        String type = ResourceConfigUtils.requireNonEmptyStringOrThrow(map.get("property"), "warning.config.item.model.select.missing_property");
        Key key = Key.withDefaultNamespace(type, "minecraft");
        SelectPropertyFactory factory = BuiltInRegistries.SELECT_PROPERTY_FACTORY.getValue(key);
        if (factory == null) {
            throw new LocalizedResourceConfigException("warning.config.item.model.select.invalid_property", type);
        }
        return factory.create(map);
    }

    public static SelectProperty fromJson(JsonObject json) {
        String type = json.get("property").getAsString();
        Key key = Key.withDefaultNamespace(type, "minecraft");
        SelectPropertyReader reader = BuiltInRegistries.SELECT_PROPERTY_READER.getValue(key);
        if (reader == null) {
            throw new IllegalArgumentException("Invalid select property type: " + key);
        }
        return reader.read(json);
    }
}
