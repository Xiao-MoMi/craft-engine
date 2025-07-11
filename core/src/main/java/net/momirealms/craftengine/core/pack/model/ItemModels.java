package net.momirealms.craftengine.core.pack.model;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.Map;

public class ItemModels {
    public static final Key EMPTY = Key.of("minecraft:empty");
    public static final Key MODEL = Key.of("minecraft:model");
    public static final Key COMPOSITE = Key.of("minecraft:composite");
    public static final Key CONDITION = Key.of("minecraft:condition");
    public static final Key RANGE_DISPATCH = Key.of("minecraft:range_dispatch");
    public static final Key SELECT = Key.of("minecraft:select");
    public static final Key SPECIAL = Key.of("minecraft:special");
    public static final Key BUNDLE_SELECTED_ITEM = Key.of("minecraft:bundle/selected_item");

    static {
        registerFactory(EMPTY, EmptyItemModel.FACTORY);
        registerReader(EMPTY, EmptyItemModel.READER);
        registerFactory(COMPOSITE, CompositeItemModel.FACTORY);
        registerReader(COMPOSITE, CompositeItemModel.READER);
        registerFactory(MODEL, BaseItemModel.FACTORY);
        registerReader(MODEL, BaseItemModel.READER);
        registerFactory(CONDITION, ConditionItemModel.FACTORY);
        registerReader(CONDITION, ConditionItemModel.READER);
        registerFactory(SPECIAL, SpecialItemModel.FACTORY);
        registerReader(SPECIAL, SpecialItemModel.READER);
        registerFactory(RANGE_DISPATCH, RangeDispatchItemModel.FACTORY);
        registerReader(RANGE_DISPATCH, RangeDispatchItemModel.READER);
        registerFactory(SELECT, SelectItemModel.FACTORY);
        registerReader(SELECT, SelectItemModel.READER);
        registerFactory(BUNDLE_SELECTED_ITEM, BundleSelectedItemModel.FACTORY);
        registerReader(BUNDLE_SELECTED_ITEM, BundleSelectedItemModel.READER);
    }

    public static void registerFactory(Key key, ItemModelFactory factory) {
        ((WritableRegistry<ItemModelFactory>) BuiltInRegistries.ITEM_MODEL_FACTORY)
                .register(ResourceKey.create(Registries.ITEM_MODEL_FACTORY.location(), key), factory);
    }

    public static void registerReader(Key key, ItemModelReader reader) {
        ((WritableRegistry<ItemModelReader>) BuiltInRegistries.ITEM_MODEL_READER)
                .register(ResourceKey.create(Registries.ITEM_MODEL_READER.location(), key), reader);
    }

    public static ItemModel fromMap(Map<String, Object> map) {
        String type = map.getOrDefault("type", "minecraft:model").toString();
        Key key = Key.withDefaultNamespace(type, "minecraft");
        ItemModelFactory factory = BuiltInRegistries.ITEM_MODEL_FACTORY.getValue(key);
        if (factory == null) {
            throw new LocalizedResourceConfigException("warning.config.item.model.invalid_type", type);
        }
        return factory.create(map);
    }

    public static ItemModel fromJson(JsonObject json) {
        String type = json.get("type").getAsString();
        Key key = Key.withDefaultNamespace(type, "minecraft");
        ItemModelReader reader = BuiltInRegistries.ITEM_MODEL_READER.getValue(key);
        if (reader == null) {
            throw new IllegalArgumentException("Invalid item model type: " + key);
        }
        return reader.read(json);
    }
}
