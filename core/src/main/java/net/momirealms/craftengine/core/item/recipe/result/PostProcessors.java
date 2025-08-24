package net.momirealms.craftengine.core.item.recipe.result;

import net.momirealms.craftengine.core.item.modifier.ItemDataModifier;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("unchecked")
public class PostProcessors {
    public static final Key APPLY_DATA = Key.of("craftengine:apply_data");

    static {
        registerPostProcessorType(APPLY_DATA, args -> {
            List<ItemDataModifier<?>> modifiers = new ArrayList<>();
            Map<String, Object> data = ResourceConfigUtils.getAsMap(args.get("data"), "data");
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                Optional.ofNullable(BuiltInRegistries.ITEM_DATA_MODIFIER_FACTORY.getValue(Key.withDefaultNamespace(entry.getKey(), Key.DEFAULT_NAMESPACE)))
                        .ifPresent(factory -> modifiers.add(factory.create(entry.getValue())));
            }
            return new ApplyItemDataPostProcessor<>(modifiers.toArray(new ItemDataModifier[0]));
        });
    }

    public static <T> PostProcessor<T> fromMap(Map<String, Object> map) {
        String type = ResourceConfigUtils.requireNonEmptyStringOrThrow(map.get("type"), "warning.config.recipe.result.post_processor.missing_type");
        Key key = Key.withDefaultNamespace(type, Key.DEFAULT_NAMESPACE);
        PostProcessor.Type<T> processor = (PostProcessor.Type<T>) BuiltInRegistries.RECIPE_POST_PROCESSOR_TYPE.getValue(key);
        if (processor == null) {
            throw new LocalizedResourceConfigException("warning.config.recipe.result.post_processor.invalid_type", type);
        }
        return processor.create(map);
    }

    public static void registerPostProcessorType(Key id, PostProcessor.Type<?> type) {
        ((WritableRegistry<PostProcessor.Type<?>>) BuiltInRegistries.RECIPE_POST_PROCESSOR_TYPE)
                .register(ResourceKey.create(Registries.RECIPE_POST_PROCESSOR_TYPE.location(), id), type);
    }
}
