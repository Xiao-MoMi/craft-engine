package net.momirealms.craftengine.core.pack.model.condition;

import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
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
    public static final Key SELECTED = Key.of("minecraft:selected");
    public static final Key USING_ITEM = Key.of("minecraft:using_item");
    public static final Key VIEW_ENTITY = Key.of("minecraft:view_entity");

    static {
        register(BROKEN, BrokenConditionProperty.FACTORY);
        register(BUNDLE_HAS_SELECTED_ITEM, SimpleConditionProperty.FACTORY);
        register(CARRIED, SimpleConditionProperty.FACTORY);
        register(DAMAGED, DamagedConditionProperty.FACTORY);
        register(EXTENDED_VIEW, SimpleConditionProperty.FACTORY);
        register(FISHING_ROD_CAST, RodCastConditionProperty.FACTORY);
        register(SELECTED, SimpleConditionProperty.FACTORY);
        register(USING_ITEM, UsingItemConditionProperty.FACTORY);
        register(VIEW_ENTITY, SimpleConditionProperty.FACTORY);
        register(CUSTOM_MODEL_DATA, CustomModelDataConditionProperty.FACTORY);
        register(HAS_COMPONENT, HasComponentConditionProperty.FACTORY);
        register(KEYBIND_DOWN, KeyBindDownConditionProperty.FACTORY);
    }

    public static void register(Key key, ConditionPropertyFactory factory) {
        Holder.Reference<ConditionPropertyFactory> holder = ((WritableRegistry<ConditionPropertyFactory>) BuiltInRegistries.CONDITION_PROPERTY_FACTORY)
                .registerForHolder(new ResourceKey<>(Registries.CONDITION_PROPERTY_FACTORY.location(), key));
        holder.bindValue(factory);
    }

    public static ConditionProperty fromMap(Map<String, Object> map) {
        String type = (String) map.get("property");
        if (type == null) {
            throw new NullPointerException("property type cannot be null");
        }
        Key key = Key.withDefaultNamespace(type, "minecraft");
        ConditionPropertyFactory factory = BuiltInRegistries.CONDITION_PROPERTY_FACTORY.getValue(key);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown property type: " + type);
        }
        return factory.create(map);
    }
}
