package net.momirealms.craftengine.core.plugin.network.protocol.recipe.legacy;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.BiFunction;

@ApiStatus.Obsolete
public final class LegacyRecipeTypes {
    private LegacyRecipeTypes() {}

    public static final LegacyRecipe.Type<LegacyShapedRecipe> SHAPED_RECIPE = register(Key.of("crafting_shaped"), LegacyShapedRecipe::read);
    public static final LegacyRecipe.Type<LegacyShapelessRecipe> SHAPELESS_RECIPE = register(Key.of("crafting_shapeless"), LegacyShapelessRecipe::read);
    public static final LegacyRecipe.Type<LegacyCustomRecipe> ARMOR_DYE = register(Key.of("crafting_special_armordye"), LegacyCustomRecipe::read);
    public static final LegacyRecipe.Type<LegacyCustomRecipe> BOOK_CLONING = register(Key.of("crafting_special_bookcloning"), LegacyCustomRecipe::read);
    public static final LegacyRecipe.Type<LegacyCustomRecipe> MAP_CLONING = register(Key.of("crafting_special_mapcloning"), LegacyCustomRecipe::read);
    public static final LegacyRecipe.Type<LegacyCustomRecipe> MAP_EXTENDING = register(Key.of("crafting_special_mapextending"), LegacyCustomRecipe::read);
    public static final LegacyRecipe.Type<LegacyCustomRecipe> FIREWORK_ROCKET = register(Key.of("crafting_special_firework_rocket"), LegacyCustomRecipe::read);
    public static final LegacyRecipe.Type<LegacyCustomRecipe> FIREWORK_STAR = register(Key.of("crafting_special_firework_star"), LegacyCustomRecipe::read);
    public static final LegacyRecipe.Type<LegacyCustomRecipe> FIREWORK_STAR_FADE = register(Key.of("crafting_special_firework_star_fade"), LegacyCustomRecipe::read);
    public static final LegacyRecipe.Type<LegacyCustomRecipe> TIPPED_ARROW = register(Key.of("crafting_special_tippedarrow"), LegacyCustomRecipe::read);
    public static final LegacyRecipe.Type<LegacyCustomRecipe> BANNER_DUPLICATE = register(Key.of("crafting_special_bannerduplicate"), LegacyCustomRecipe::read);
    public static final LegacyRecipe.Type<LegacyCustomRecipe> SHIELD_DECORATION = register(Key.of("crafting_special_shielddecoration"), LegacyCustomRecipe::read);
    public static final LegacyRecipe.Type<LegacyCustomRecipe> SHULKER_BOX_COLORING = register(Key.of("crafting_special_shulkerboxcoloring"), LegacyCustomRecipe::read);
    public static final LegacyRecipe.Type<LegacyCustomRecipe> SUSPICIOUS_STEW = register(Key.of("crafting_special_suspiciousstew"), LegacyCustomRecipe::read);
    public static final LegacyRecipe.Type<LegacyCustomRecipe> REPAIR_ITEM = register(Key.of("crafting_special_repairitem"), LegacyCustomRecipe::read);
    public static final LegacyRecipe.Type<LegacyCookingRecipe> SMELTING_RECIPE = register(Key.of("smelting"), LegacyCookingRecipe::read);
    public static final LegacyRecipe.Type<LegacyCookingRecipe> BLASTING_RECIPE = register(Key.of("blasting"), LegacyCookingRecipe::read);
    public static final LegacyRecipe.Type<LegacyCookingRecipe> SMOKING_RECIPE = register(Key.of("smoking"), LegacyCookingRecipe::read);
    public static final LegacyRecipe.Type<LegacyCookingRecipe> CAMPFIRE_COOKING_RECIPE = register(Key.of("campfire_cooking"), LegacyCookingRecipe::read);
    public static final LegacyRecipe.Type<LegacyStoneCuttingRecipe> STONECUTTER = register(Key.of("stonecutting"), LegacyStoneCuttingRecipe::read);
    public static final LegacyRecipe.Type<LegacySmithingTransformRecipe> SMITHING_TRANSFORM = register(Key.of("smithing_transform"), LegacySmithingTransformRecipe::read);
    public static final LegacyRecipe.Type<LegacySmithingTrimRecipe> SMITHING_TRIM = register(Key.of("smithing_trim"), LegacySmithingTrimRecipe::read);
    public static final LegacyRecipe.Type<LegacyCustomRecipe> DECORATED_POT_RECIPE = register(Key.of("crafting_decorated_pot"), LegacyCustomRecipe::read);

    public static void init() {
    }

    public static <T extends LegacyRecipe> LegacyRecipe.Type<T> register(Key key, BiFunction<FriendlyByteBuf, FriendlyByteBuf.Reader<Item>, T> function) {
        LegacyRecipe.Type<T> type = new LegacyRecipe.Type<>(key, function);
        ((WritableRegistry<LegacyRecipe.Type<? extends LegacyRecipe>>) BuiltInRegistries.LEGACY_RECIPE_TYPE)
                .register(ResourceKey.create(Registries.LEGACY_RECIPE_TYPE.location(), key), type);
        return type;
    }
}
