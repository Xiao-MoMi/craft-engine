package net.momirealms.craftengine.core.pack.model.definition;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.pack.model.generation.ModelGenerationHolder;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;

public final class ItemModels {
    public static final ItemModelType<EmptyItemModel> EMPTY = register(Key.of("empty"), EmptyItemModel.FACTORY, EmptyItemModel.READER);
    public static final ItemModelType<BaseItemModel> MODEL = register(Key.of("model"), BaseItemModel.FACTORY, BaseItemModel.READER);
    public static final ItemModelType<CompositeItemModel> COMPOSITE = register(Key.of("composite"), CompositeItemModel.FACTORY, CompositeItemModel.READER);
    public static final ItemModelType<ConditionItemModel> CONDITION = register(Key.of("condition"), ConditionItemModel.FACTORY, ConditionItemModel.READER);
    public static final ItemModelType<RangeDispatchItemModel> RANGE_DISPATCH = register(Key.of("range_dispatch"), RangeDispatchItemModel.FACTORY, RangeDispatchItemModel.READER);
    public static final ItemModelType<SelectItemModel> SELECT = register(Key.of("select"), SelectItemModel.FACTORY, SelectItemModel.READER);
    public static final ItemModelType<SpecialItemModel> SPECIAL = register(Key.of("special"), SpecialItemModel.FACTORY, SpecialItemModel.READER);
    public static final ItemModelType<BundleSelectedItemModel> BUNDLE_SELECTED_ITEM = register(Key.of("bundle/selected_item"), BundleSelectedItemModel.FACTORY, BundleSelectedItemModel.READER);

    private ItemModels() {}

    public static <T extends ItemModel> ItemModelType<T> register(Key key, ItemModelFactory<T> factory, ItemModelReader<T> reader) {
        ItemModelType<T> type = new ItemModelType<>(key, factory, reader);
        ((WritableRegistry<ItemModelType<? extends ItemModel>>) BuiltInRegistries.ITEM_MODEL_TYPE)
                .register(ResourceKey.create(Registries.ITEM_MODEL_TYPE.location(), key), type);
        return type;
    }

    public static ItemModel fromConfig(Pack pack, Path path, ConfigSection section, Consumer<ModelGenerationHolder> generations) {
        String typeName = section.getString("type", "minecraft:model");
        Key type = Key.of(typeName);
        ItemModelType<? extends ItemModel> itemModelType = BuiltInRegistries.ITEM_MODEL_TYPE.getValue(type);
        if (itemModelType == null) {
            throw new KnownResourceException("resource.item.model_definition.unknown_type", section.assemblePath("type"), type.asString());
        }
        return itemModelType.factory().create(pack, path, section, generations);
    }

    public static ItemModel fromConfig(Pack pack, Path path, ConfigValue value, Consumer<ModelGenerationHolder> generations) {
        if (value.is(Map.class)) {
            return fromConfig(pack, path, value.getAsSection(), generations);
        }
        return new BaseItemModel(value.getAsAssetPath());
    }

    public static ItemModel fromJson(JsonObject json) {
        String type = json.get("type").getAsString();
        Key key = Key.withDefaultNamespace(type, "minecraft");
        ItemModelType<? extends ItemModel> itemModelType = BuiltInRegistries.ITEM_MODEL_TYPE.getValue(key);
        if (itemModelType == null) {
            throw new IllegalArgumentException("Invalid item model type: " + key);
        }
        return itemModelType.reader().read(json);
    }
}
