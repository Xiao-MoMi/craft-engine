package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;
import net.momirealms.craftengine.core.util.VersionHelper;

public final class RecipeSerializers {
    public static final Key SHAPED = Key.of("minecraft:shaped");
    public static final Key SHAPELESS = Key.of("minecraft:shapeless");
    public static final Key DYE = Key.of("minecraft:dye");
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
        if (VersionHelper.isOrAbove26_1) {
            register(DYE, CustomDyeRecipe.SERIALIZER);
            register(Key.of("crafting_dye"), CustomDyeRecipe.SERIALIZER);
        }
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
    public static <R extends Recipe> void register(Key key, RecipeSerializer<R> serializer) {
        WritableRegistry<RecipeSerializer<R>> registry = (WritableRegistry) BuiltInRegistries.RECIPE_SERIALIZER;
        registry.register(ResourceKey.create(Registries.RECIPE_SERIALIZER.location(), key), serializer);
    }

    @SuppressWarnings("unchecked")
    public static <R extends Recipe> Recipe fromConfig(Key id, ConfigSection section) {
        String type = section.getNonEmptyString("type");
        Key key = Key.minecraft(type);
        RecipeSerializer<R> factory = (RecipeSerializer<R>) BuiltInRegistries.RECIPE_SERIALIZER.getValue(key);
        if (factory == null) {
            throw new KnownResourceException("resource.recipe.unknown_type", section.assemblePath("type"), key.asString());
        }
        return factory.readConfig(id, section);
    }
}
