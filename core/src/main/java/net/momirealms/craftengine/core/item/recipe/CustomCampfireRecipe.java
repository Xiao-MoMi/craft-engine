package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CustomCampfireRecipe<T> extends CustomCookingRecipe<T> {
    public static final Factory<?> FACTORY = new Factory<>();

    public CustomCampfireRecipe(Key id, CookingRecipeCategory category, String group, Ingredient<T> ingredient, int cookingTime, float experience, CustomRecipeResult<T> result) {
        super(id, category, group, ingredient, cookingTime, experience, result);
    }

    @Override
    public @NotNull Key type() {
        return RecipeTypes.CAMPFIRE_COOKING;
    }

    public static class Factory<A> implements RecipeFactory<CustomCampfireRecipe<A>> {

        @SuppressWarnings({"unchecked", "rawtypes", "DuplicatedCode"})
        @Override
        public Recipe<CustomCampfireRecipe<A>> create(Key id, Map<String, Object> arguments) {
            CookingRecipeCategory recipeCategory = arguments.containsKey("category") ? CookingRecipeCategory.valueOf(arguments.get("category").toString().toUpperCase(Locale.ENGLISH)) : null;
            String group = arguments.containsKey("group") ? arguments.get("group").toString() : null;
            int cookingTime = MiscUtils.getAsInt(arguments.getOrDefault("time", 80));
            float experience = MiscUtils.getAsFloat(arguments.getOrDefault("experience", 0.0f));
            List<String> items = MiscUtils.getAsStringList(arguments.get("ingredient"));
            Set<Holder<Key>> holders = new HashSet<>();
            for (String item : items) {
                if (item.charAt(0) == '#') {
                    holders.addAll(CraftEngine.instance().itemManager().tagToItems(Key.of(item.substring(1))));
                } else {
                    holders.add(BuiltInRegistries.OPTIMIZED_ITEM_ID.get(Key.of(item)).orElseThrow(() -> new IllegalArgumentException("Invalid vanilla/custom item: " + item)));
                }
            }
            return new CustomCampfireRecipe(
                    id,
                    recipeCategory,
                    group,
                    Ingredient.of(holders),
                    cookingTime,
                    experience,
                    parseResult(arguments)
            );
        }
    }
}
