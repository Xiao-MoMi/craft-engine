package net.momirealms.craftengine.core.item.recipe.result;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

public final class PostProcessors {
    public static final PostProcessorType<ApplyItemDataPostProcessor> APPLY_DATA = register(Key.ce("apply_data"), ApplyItemDataPostProcessor.FACTORY);

    private PostProcessors() {}

    public static PostProcessor fromConfig(ConfigSection section) {
        String type = section.getNonEmptyString("type");
        Key key = Key.ce(type);
        PostProcessorType<? extends PostProcessor> processorType = BuiltInRegistries.RECIPE_POST_PROCESSOR_TYPE.getValue(key);
        if (processorType == null) {
            throw new KnownResourceException("resource.recipe.post_processor.unknown_type", section.assemblePath("type"), key.asString());
        }
        return processorType.factory().create(section);
    }

    public static <T extends PostProcessor> PostProcessorType<T> register(Key id, PostProcessorFactory<T> factory) {
        PostProcessorType<T> type = new PostProcessorType<>(id, factory);
        ((WritableRegistry<PostProcessorType<?>>) BuiltInRegistries.RECIPE_POST_PROCESSOR_TYPE)
                .register(ResourceKey.create(Registries.RECIPE_POST_PROCESSOR_TYPE.location(), id), type);
        return type;
    }
}
