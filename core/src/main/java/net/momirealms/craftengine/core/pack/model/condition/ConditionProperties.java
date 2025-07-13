package net.momirealms.craftengine.core.pack.model.condition;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.Map;

public class ConditionProperties {
    public static final Key BROKEN = Key.of("minecraft:broken");
    public static final Key BUNDLE_HAS_SELECTED_ITEM = Key.of("minecraft:bundle/has_selected_item");
    public static final Key CARRIED = Key.of("minecraft:carried");
    public static final Key CUSTOM_MODEL_DATA = Key.of("minecraft:custom_model_data");
    public static final Key DAMAGED = Key.of("minecraft:damaged");
    public static final Key EXTENDED_VIEW = Key.of("minecraft:extended_view");
    public static final Key FISHING_ROD_CAST = Key.of("minecraft:fishing_rod/cast");
    public static final Key HAS_COMPONENT = Key.of("minecraft:has_component");
    public static final Key KEYBIND_DOWN = Key.of("minecraft:keybind_down");
    public static final Key COMPONENT = Key.of("minecraft:component");
    public static final Key SELECTED = Key.of("minecraft:selected");
    public static final Key USING_ITEM = Key.of("minecraft:using_item");
    public static final Key VIEW_ENTITY = Key.of("minecraft:view_entity");

    static {
        registerFactory(BROKEN, BrokenConditionProperty.FACTORY);
        registerReader(BROKEN, BrokenConditionProperty.READER);
        registerFactory(BUNDLE_HAS_SELECTED_ITEM, SimpleConditionProperty.FACTORY);
        registerReader(BUNDLE_HAS_SELECTED_ITEM, SimpleConditionProperty.READER);
        registerFactory(CARRIED, SimpleConditionProperty.FACTORY);
        registerReader(CARRIED, SimpleConditionProperty.READER);
        registerFactory(DAMAGED, DamagedConditionProperty.FACTORY);
        registerReader(DAMAGED, DamagedConditionProperty.READER);
        registerFactory(EXTENDED_VIEW, SimpleConditionProperty.FACTORY);
        registerReader(EXTENDED_VIEW, SimpleConditionProperty.READER);
        registerFactory(FISHING_ROD_CAST, RodCastConditionProperty.FACTORY);
        registerReader(FISHING_ROD_CAST, RodCastConditionProperty.READER);
        registerFactory(SELECTED, SimpleConditionProperty.FACTORY);
        registerReader(SELECTED, SimpleConditionProperty.READER);
        registerFactory(USING_ITEM, UsingItemConditionProperty.FACTORY);
        registerReader(USING_ITEM, UsingItemConditionProperty.READER);
        registerFactory(VIEW_ENTITY, SimpleConditionProperty.FACTORY);
        registerReader(VIEW_ENTITY, SimpleConditionProperty.READER);
        registerFactory(CUSTOM_MODEL_DATA, CustomModelDataConditionProperty.FACTORY);
        registerReader(CUSTOM_MODEL_DATA, CustomModelDataConditionProperty.READER);
        registerFactory(HAS_COMPONENT, HasComponentConditionProperty.FACTORY);
        registerReader(HAS_COMPONENT, HasComponentConditionProperty.READER);
        registerFactory(KEYBIND_DOWN, KeyBindDownConditionProperty.FACTORY);
        registerReader(KEYBIND_DOWN, KeyBindDownConditionProperty.READER);
        registerFactory(COMPONENT, ComponentConditionProperty.FACTORY);
        registerReader(COMPONENT, ComponentConditionProperty.READER);
    }

    public static void registerFactory(Key key, ConditionPropertyFactory factory) {
        ((WritableRegistry<ConditionPropertyFactory>) BuiltInRegistries.CONDITION_PROPERTY_FACTORY)
                .register(ResourceKey.create(Registries.CONDITION_PROPERTY_FACTORY.location(), key), factory);
    }

    public static void registerReader(Key key, ConditionPropertyReader reader) {
        ((WritableRegistry<ConditionPropertyReader>) BuiltInRegistries.CONDITION_PROPERTY_READER)
                .register(ResourceKey.create(Registries.CONDITION_PROPERTY_READER.location(), key), reader);
    }

    public static ConditionProperty fromMap(Map<String, Object> map) {
        String type = ResourceConfigUtils.requireNonEmptyStringOrThrow(map.get("property"), "warning.config.item.model.condition.missing_property");
        Key key = Key.withDefaultNamespace(type, "minecraft");
        ConditionPropertyFactory factory = BuiltInRegistries.CONDITION_PROPERTY_FACTORY.getValue(key);
        if (factory == null) {
            throw new LocalizedResourceConfigException("warning.config.item.model.condition.invalid_property", type);
        }
        return factory.create(map);
    }

    public static ConditionProperty fromJson(JsonObject json) {
        String type = json.get("property").getAsString();
        Key key = Key.withDefaultNamespace(type, "minecraft");
        ConditionPropertyReader reader = BuiltInRegistries.CONDITION_PROPERTY_READER.getValue(key);
        if (reader == null) {
            throw new IllegalArgumentException("Invalid condition property type: " + key);
        }
        return reader.read(json);
    }
}
