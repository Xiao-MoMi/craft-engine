package net.momirealms.craftengine.core.pack.model.definition.condition;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

public final class ConditionProperties {
    public static final ConditionPropertyType<BrokenConditionProperty> BROKEN = register(Key.of("broken"), BrokenConditionProperty.FACTORY, BrokenConditionProperty.READER);
    public static final ConditionPropertyType<SimpleConditionProperty> BUNDLE_HAS_SELECTED_ITEM = register(Key.of("bundle/has_selected_item"), SimpleConditionProperty.FACTORY, SimpleConditionProperty.READER);
    public static final ConditionPropertyType<SimpleConditionProperty> CARRIED = register(Key.of("carried"), SimpleConditionProperty.FACTORY, SimpleConditionProperty.READER);
    public static final ConditionPropertyType<ComponentConditionProperty> COMPONENT = register(Key.of("component"), ComponentConditionProperty.FACTORY, ComponentConditionProperty.READER);
    public static final ConditionPropertyType<DamagedConditionProperty> DAMAGED = register(Key.of("damaged"), DamagedConditionProperty.FACTORY, DamagedConditionProperty.READER);
    public static final ConditionPropertyType<SimpleConditionProperty> EXTENDED_VIEW = register(Key.of("extended_view"), SimpleConditionProperty.FACTORY, SimpleConditionProperty.READER);
    public static final ConditionPropertyType<RodCastConditionProperty> FISHING_ROD_CAST = register(Key.of("fishing_rod/cast"), RodCastConditionProperty.FACTORY, RodCastConditionProperty.READER);
    public static final ConditionPropertyType<HasComponentConditionProperty> HAS_COMPONENT = register(Key.of("has_component"), HasComponentConditionProperty.FACTORY, HasComponentConditionProperty.READER);
    public static final ConditionPropertyType<KeyBindDownConditionProperty> KEYBIND_DOWN = register(Key.of("keybind_down"), KeyBindDownConditionProperty.FACTORY, KeyBindDownConditionProperty.READER);
    public static final ConditionPropertyType<SimpleConditionProperty> SELECTED = register(Key.of("selected"), SimpleConditionProperty.FACTORY, SimpleConditionProperty.READER);
    public static final ConditionPropertyType<UsingItemConditionProperty> USING_ITEM = register(Key.of("using_item"), UsingItemConditionProperty.FACTORY, UsingItemConditionProperty.READER);
    public static final ConditionPropertyType<SimpleConditionProperty> VIEW_ENTITY = register(Key.of("view_entity"), SimpleConditionProperty.FACTORY, SimpleConditionProperty.READER);
    public static final ConditionPropertyType<CustomModelDataConditionProperty> CUSTOM_MODEL_DATA = register(Key.of("custom_model_data"), CustomModelDataConditionProperty.FACTORY, CustomModelDataConditionProperty.READER);

    private ConditionProperties() {}

    public static <T extends ConditionProperty> ConditionPropertyType<T> register(Key id, ConditionPropertyFactory<T> factory, ConditionPropertyReader<T> reader) {
        ConditionPropertyType<T> type = new ConditionPropertyType<>(id, factory, reader);
        ((WritableRegistry<ConditionPropertyType<? extends ConditionProperty>>) BuiltInRegistries.CONDITION_PROPERTY_TYPE)
                .register(ResourceKey.create(Registries.CONDITION_PROPERTY_TYPE.location(), id), type);
        return type;
    }

    public static ConditionProperty fromConfig(ConfigSection section) {
        String typeName = section.getNonEmptyString("property");
        Key type = Key.minecraft(typeName);
        ConditionPropertyType<? extends ConditionProperty> propertyType = BuiltInRegistries.CONDITION_PROPERTY_TYPE.getValue(type);
        if (propertyType == null) {
            throw new KnownResourceException("resource.item.model_definition.condition.unknown_type", section.assemblePath("property"), type.asString());
        }
        return propertyType.factory().create(section);
    }

    public static ConditionProperty fromJson(JsonObject json) {
        String type = json.get("property").getAsString();
        Key key = Key.withDefaultNamespace(type, "minecraft");
        ConditionPropertyType<? extends ConditionProperty> propertyType = BuiltInRegistries.CONDITION_PROPERTY_TYPE.getValue(key);
        if (propertyType == null) {
            throw new IllegalArgumentException("Invalid condition property type: " + key);
        }
        return propertyType.reader().read(json);
    }
}
