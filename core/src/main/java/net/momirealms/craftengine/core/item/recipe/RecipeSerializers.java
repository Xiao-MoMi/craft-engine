package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.Map;

public final class RecipeSerializers {
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
        register(SHAPED, CustomShapedRecipe.SERIALIZER);
        register(Key.of("crafting_shaped"), CustomShapedRecipe.SERIALIZER);
        register(SHAPELESS, CustomShapelessRecipe.SERIALIZER);
        register(Key.of("crafting_shapeless"), CustomShapelessRecipe.SERIALIZER);
        register(SMELTING, CustomSmeltingRecipe.SERIALIZER);
        register(SMOKING, CustomSmokingRecipe.SERIALIZER);
        register(BLASTING, CustomBlastingRecipe.SERIALIZER);
        register(CAMPFIRE_COOKING, CustomCampfireRecipe.SERIALIZER);
        register(STONECUTTING, CustomStoneCuttingRecipe.SERIALIZER);
        register(SMITHING_TRANSFORM, CustomSmithingTransformRecipe.SERIALIZER);
        register(SMITHING_TRIM, CustomSmithingTrimRecipe.SERIALIZER);
        register(BREWING, CustomBrewingRecipe.SERIALIZER);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T, R extends Recipe<T>> void register(Key key, RecipeSerializer<T, R> serializer) {
        WritableRegistry<RecipeSerializer<T, R>> registry = (WritableRegistry) BuiltInRegistries.RECIPE_SERIALIZER;
        registry.register(ResourceKey.create(Registries.RECIPE_FACTORY.location(), key), serializer);
    }

    @SuppressWarnings("unchecked")
    public static <T, R extends Recipe<T>> Recipe<T> fromMap(Key id, Map<String, Object> map) {
        String type = ResourceConfigUtils.requireNonEmptyStringOrThrow(map.get("type"), "warning.config.recipe.missing_type");
        Key key = Key.withDefaultNamespace(type, "minecraft");
        RecipeSerializer<T, R> factory = (RecipeSerializer<T, R>) BuiltInRegistries.RECIPE_SERIALIZER.getValue(key);
        if (factory == null) {
            throw new LocalizedResourceConfigException("warning.config.recipe.invalid_type", type);
        }
        return factory.readMap(id, map);
    }
}
