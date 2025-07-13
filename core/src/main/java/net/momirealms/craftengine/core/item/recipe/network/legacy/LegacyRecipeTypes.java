package net.momirealms.craftengine.core.item.recipe.network.legacy;

import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Obsolete
public final class LegacyRecipeTypes {
    private LegacyRecipeTypes() {}

    public static final Key SHAPED_RECIPE = Key.of("crafting_shaped");
    public static final Key SHAPELESS_RECIPE = Key.of("crafting_shapeless");
    public static final Key ARMOR_DYE = Key.of("crafting_special_armordye");
    public static final Key BOOK_CLONING = Key.of("crafting_special_bookcloning");
    public static final Key MAP_CLONING = Key.of("crafting_special_mapcloning");
    public static final Key MAP_EXTENDING = Key.of("crafting_special_mapextending");
    public static final Key FIREWORK_ROCKET = Key.of("crafting_special_firework_rocket");
    public static final Key FIREWORK_STAR = Key.of("crafting_special_firework_star");
    public static final Key FIREWORK_STAR_FADE = Key.of("crafting_special_firework_star_fade");
    public static final Key TIPPED_ARROW = Key.of("crafting_special_tippedarrow");
    public static final Key BANNER_DUPLICATE = Key.of("crafting_special_bannerduplicate");
    public static final Key SHIELD_DECORATION = Key.of("crafting_special_shielddecoration");
    public static final Key SHULKER_BOX_COLORING = Key.of("crafting_special_shulkerboxcoloring");
    public static final Key SUSPICIOUS_STEW = Key.of("crafting_special_suspiciousstew");
    public static final Key REPAIR_ITEM = Key.of("crafting_special_repairitem");
    public static final Key SMELTING_RECIPE = Key.of("smelting");
    public static final Key BLASTING_RECIPE = Key.of("blasting");
    public static final Key SMOKING_RECIPE = Key.of("smoking");
    public static final Key CAMPFIRE_COOKING_RECIPE = Key.of("campfire_cooking");
    public static final Key STONECUTTER = Key.of("stonecutting");
    public static final Key SMITHING_TRANSFORM = Key.of("smithing_transform");
    public static final Key SMITHING_TRIM = Key.of("smithing_trim");
    public static final Key DECORATED_POT_RECIPE = Key.of("crafting_decorated_pot");

    public static void register() {
        register(SHAPED_RECIPE, new LegacyRecipe.Type(LegacyShapedRecipe::read));
        register(SHAPELESS_RECIPE, new LegacyRecipe.Type(LegacyShapelessRecipe::read));
        register(ARMOR_DYE, new LegacyRecipe.Type(LegacyCustomRecipe::read));
        register(BOOK_CLONING, new LegacyRecipe.Type(LegacyCustomRecipe::read));
        register(MAP_CLONING, new LegacyRecipe.Type(LegacyCustomRecipe::read));
        register(MAP_EXTENDING, new LegacyRecipe.Type(LegacyCustomRecipe::read));
        register(FIREWORK_ROCKET, new LegacyRecipe.Type(LegacyCustomRecipe::read));
        register(FIREWORK_STAR, new LegacyRecipe.Type(LegacyCustomRecipe::read));
        register(FIREWORK_STAR_FADE, new LegacyRecipe.Type(LegacyCustomRecipe::read));
        register(TIPPED_ARROW, new LegacyRecipe.Type(LegacyCustomRecipe::read));
        register(BANNER_DUPLICATE, new LegacyRecipe.Type(LegacyCustomRecipe::read));
        register(SHIELD_DECORATION, new LegacyRecipe.Type(LegacyCustomRecipe::read));
        register(SHULKER_BOX_COLORING, new LegacyRecipe.Type(LegacyCustomRecipe::read));
        register(SUSPICIOUS_STEW, new LegacyRecipe.Type(LegacyCustomRecipe::read));
        register(REPAIR_ITEM, new LegacyRecipe.Type(LegacyCustomRecipe::read));
        register(SMELTING_RECIPE, new LegacyRecipe.Type(LegacyCookingRecipe::read));
        register(BLASTING_RECIPE, new LegacyRecipe.Type(LegacyCookingRecipe::read));
        register(SMOKING_RECIPE, new LegacyRecipe.Type(LegacyCookingRecipe::read));
        register(CAMPFIRE_COOKING_RECIPE, new LegacyRecipe.Type(LegacyCookingRecipe::read));
        register(STONECUTTER, new LegacyRecipe.Type(LegacyStoneCuttingRecipe::read));
        register(SMITHING_TRANSFORM, new LegacyRecipe.Type(LegacySmithingTransformRecipe::read));
        register(SMITHING_TRIM, new LegacyRecipe.Type(LegacySmithingTrimRecipe::read));
        register(DECORATED_POT_RECIPE, new LegacyRecipe.Type(LegacyCustomRecipe::read));
    }

    public static void register(Key key, LegacyRecipe.Type type) {
        ((WritableRegistry<LegacyRecipe.Type>) BuiltInRegistries.LEGACY_RECIPE_TYPE)
                .register(ResourceKey.create(Registries.LEGACY_RECIPE_TYPE.location(), key), type);
    }
}
