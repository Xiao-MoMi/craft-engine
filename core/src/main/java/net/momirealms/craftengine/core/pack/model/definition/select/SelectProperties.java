package net.momirealms.craftengine.core.pack.model.definition.select;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

public final class SelectProperties {
    public static final SelectPropertyType<BlockStateSelectProperty> BLOCK_STATE = register(Key.of("block_state"), BlockStateSelectProperty.FACTORY, BlockStateSelectProperty.READER);
    public static final SelectPropertyType<ChargeTypeSelectProperty> CHARGE_TYPE = register(Key.of("charge_type"), ChargeTypeSelectProperty.FACTORY, ChargeTypeSelectProperty.READER);
    public static final SelectPropertyType<ComponentSelectProperty> COMPONENT = register(Key.of("component"), ComponentSelectProperty.FACTORY, ComponentSelectProperty.READER);
    public static final SelectPropertyType<SimpleSelectProperty> CONTEXT_DIMENSION = register(Key.of("context_dimension"), SimpleSelectProperty.FACTORY, SimpleSelectProperty.READER);
    public static final SelectPropertyType<SimpleSelectProperty> CONTEXT_ENTITY_TYPE = register(Key.of("context_entity_type"), SimpleSelectProperty.FACTORY, SimpleSelectProperty.READER);
    public static final SelectPropertyType<DisplayContextSelectProperty> DISPLAY_CONTEXT = register(Key.of("display_context"), DisplayContextSelectProperty.FACTORY, DisplayContextSelectProperty.READER);
    public static final SelectPropertyType<LocalTimeSelectProperty> LOCAL_TIME = register(Key.of("local_time"), LocalTimeSelectProperty.FACTORY, LocalTimeSelectProperty.READER);
    public static final SelectPropertyType<MainHandSelectProperty> MAIN_HAND = register(Key.of("main_hand"), MainHandSelectProperty.FACTORY, MainHandSelectProperty.READER);
    public static final SelectPropertyType<TrimMaterialSelectProperty> TRIM_MATERIAL = register(Key.of("trim_material"), TrimMaterialSelectProperty.FACTORY, TrimMaterialSelectProperty.READER);
    public static final SelectPropertyType<CustomModelDataSelectProperty> CUSTOM_MODEL_DATA = register(Key.of("custom_model_data"), CustomModelDataSelectProperty.FACTORY, CustomModelDataSelectProperty.READER);

    private SelectProperties() {}

    public static <T extends SelectProperty> SelectPropertyType<T> register(Key id, SelectPropertyFactory<T> factory, SelectPropertyReader<T> reader) {
        SelectPropertyType<T> type = new SelectPropertyType<>(id, factory, reader);
        ((WritableRegistry<SelectPropertyType<? extends SelectProperty>>) BuiltInRegistries.SELECT_PROPERTY_TYPE)
                .register(ResourceKey.create(Registries.SELECT_PROPERTY_TYPE.location(), id), type);
        return type;
    }

    public static SelectProperty fromConfig(ConfigSection section) {
        String typeName = section.getNonEmptyString("property");
        Key type = Key.minecraft(typeName);
        SelectPropertyType<? extends SelectProperty> selectPropertyType = BuiltInRegistries.SELECT_PROPERTY_TYPE.getValue(type);
        if (selectPropertyType == null) {
            throw new KnownResourceException("resource.item.model_definition.select.unknown_type", section.assemblePath("property"), type.asString());
        }
        return selectPropertyType.factory().create(section);
    }

    public static SelectProperty fromJson(JsonObject json) {
        String type = json.get("property").getAsString();
        Key key = Key.withDefaultNamespace(type, "minecraft");
        SelectPropertyType<? extends SelectProperty> selectPropertyType = BuiltInRegistries.SELECT_PROPERTY_TYPE.getValue(key);
        if (selectPropertyType == null) {
            throw new IllegalArgumentException("Invalid select property type: " + key);
        }
        return selectPropertyType.reader().read(json);
    }
}
