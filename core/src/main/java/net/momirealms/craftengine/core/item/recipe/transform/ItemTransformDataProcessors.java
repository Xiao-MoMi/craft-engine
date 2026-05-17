package net.momirealms.craftengine.core.item.recipe.transform;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

public final class ItemTransformDataProcessors {
    public static final ItemTransformDataProcessor.Type<KeepComponents> KEEP_COMPONENTS = register(Key.ce("keep_components"), KeepComponents.FACTORY);
    public static final ItemTransformDataProcessor.Type<KeepTags> KEEP_TAGS = register(Key.ce("keep_tags"), KeepTags.FACTORY);
    public static final ItemTransformDataProcessor.Type<KeepCustomData> KEEP_CUSTOM_DATA = register(Key.ce("keep_custom_data"), KeepCustomData.FACTORY);
    public static final ItemTransformDataProcessor.Type<MergeEnchantments> MERGE_ENCHANTMENTS = register(Key.ce("merge_enchantments"), MergeEnchantments.FACTORY);
    public static final ItemTransformDataProcessor.Type<ApplyData> APPLY_DATA = register(Key.ce("apply_data"), ApplyData.FACTORY);

    private ItemTransformDataProcessors() {}

    public static ItemTransformDataProcessor fromConfig(ConfigValue value) {
        return fromConfig(value.getAsSection());
    }

    public static ItemTransformDataProcessor fromConfig(ConfigSection section) {
        String type = section.getNonEmptyString("type");
        Key key = Key.ce(type);
        ItemTransformDataProcessor.Type<? extends ItemTransformDataProcessor> processorType = BuiltInRegistries.TRANSFORM_PROCESSOR_TYPE.getValue(key);
        if (processorType == null) {
            throw new KnownResourceException("resource.recipe.transform_processor.unknown_type", section.assemblePath("type"), key.asString());
        }
        return processorType.factory().create(section);
    }

    public static <T extends ItemTransformDataProcessor> ItemTransformDataProcessor.Type<T> register(Key key, ItemTransformDataProcessor.Factory<T> factory) {
        ItemTransformDataProcessor.Type<T> type = new ItemTransformDataProcessor.Type<>(key, factory);
        ((WritableRegistry<ItemTransformDataProcessor.Type<? extends ItemTransformDataProcessor>>) BuiltInRegistries.TRANSFORM_PROCESSOR_TYPE)
                .register(ResourceKey.create(Registries.TRANSFORM_PROCESSOR_TYPE.location(), key), type);
        return type;
    }
}