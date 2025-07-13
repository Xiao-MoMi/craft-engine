package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.Map;

public class RecipeTypes {
    public static final Key SHAPED = Key.of("minecraft:shaped");
    public static final Key SHAPELESS = Key.of("minecraft:shapeless");
    public static final Key SMELTING = Key.of("minecraft:smelting");
    public static final Key BLASTING = Key.of("minecraft:blasting");
    public static final Key SMOKING = Key.of("minecraft:smoking");
    public static final Key CAMPFIRE_COOKING = Key.of("minecraft:campfire_cooking");
    public static final Key STONECUTTING = Key.of("minecraft:stonecutting");
    public static final Key SMITHING_TRANSFORM = Key.of("minecraft:smithing_transform");
    public static final Key SMITHING_TRIM = Key.of("minecraft:smithing_trim");
    public static final Key BREWING = Key.of("minecraft:brewing");

    static {
        register(SHAPED, CustomShapedRecipe.FACTORY);
        register(SHAPELESS, CustomShapelessRecipe.FACTORY);
        register(SMELTING, CustomSmeltingRecipe.FACTORY);
        register(SMOKING, CustomSmokingRecipe.FACTORY);
        register(BLASTING, CustomBlastingRecipe.FACTORY);
        register(CAMPFIRE_COOKING, CustomCampfireRecipe.FACTORY);
        register(STONECUTTING, CustomStoneCuttingRecipe.FACTORY);
        register(SMITHING_TRANSFORM, CustomSmithingTransformRecipe.FACTORY);
        register(SMITHING_TRIM, CustomSmithingTrimRecipe.FACTORY);
        register(BREWING, CustomBrewingRecipe.FACTORY);
    }

    public static <T> void register(Key key, RecipeFactory<T> factory) {
        ((WritableRegistry<RecipeFactory<?>>) BuiltInRegistries.RECIPE_FACTORY)
                .register(ResourceKey.create(Registries.RECIPE_FACTORY.location(), key), factory);
    }

    @SuppressWarnings("unchecked")
    public static <T> Recipe<T> fromMap(Key id, Map<String, Object> map) {
        String type = ResourceConfigUtils.requireNonEmptyStringOrThrow(map.get("type"), "warning.config.recipe.missing_type");
        Key key = Key.withDefaultNamespace(type, "minecraft");
        RecipeFactory<T> factory = (RecipeFactory<T>) BuiltInRegistries.RECIPE_FACTORY.getValue(key);
        if (factory == null) {
            throw new LocalizedResourceConfigException("warning.config.recipe.invalid_type", type);
        }
        return factory.create(id, map);
    }
}
