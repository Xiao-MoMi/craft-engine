package net.momirealms.craftengine.core.plugin.network.protocol.recipe.modern.display;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.function.BiFunction;

public final class RecipeDisplayTypes {
    private RecipeDisplayTypes() {}

    public static final RecipeDisplay.Type<ShapelessCraftingRecipeDisplay> CRAFTING_SHAPELESS = register(Key.of("crafting_shapeless"), ShapelessCraftingRecipeDisplay::read);
    public static final RecipeDisplay.Type<ShapedCraftingRecipeDisplay> CRAFTING_SHAPED = register(Key.of("crafting_shaped"), ShapedCraftingRecipeDisplay::read);
    public static final RecipeDisplay.Type<FurnaceRecipeDisplay> FURNACE = register(Key.of("furnace"), FurnaceRecipeDisplay::read);
    public static final RecipeDisplay.Type<StonecutterRecipeDisplay> STONECUTTER = register(Key.of("stonecutter"), StonecutterRecipeDisplay::read);
    public static final RecipeDisplay.Type<SmithingRecipeDisplay> SMITHING = register(Key.of("smithing"), SmithingRecipeDisplay::read);

    public static void init() {
    }

    public static <T extends RecipeDisplay> RecipeDisplay.Type<T> register(Key key, BiFunction<FriendlyByteBuf, FriendlyByteBuf.Reader<Item>, T> function) {
        RecipeDisplay.Type<T> type = new RecipeDisplay.Type<>(key, function);
        ((WritableRegistry<RecipeDisplay.Type<? extends RecipeDisplay>>) BuiltInRegistries.RECIPE_DISPLAY_TYPE)
                .register(ResourceKey.create(Registries.RECIPE_DISPLAY_TYPE.location(), key), type);
        return type;
    }
}
