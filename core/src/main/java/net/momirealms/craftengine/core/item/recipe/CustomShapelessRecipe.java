package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.item.recipe.input.CraftingInput;
import net.momirealms.craftengine.core.item.recipe.input.RecipeInput;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CustomShapelessRecipe<T> extends CraftingTableRecipe<T> {
    public static final Factory<?> FACTORY = new Factory<>();
    private final List<Ingredient<T>> ingredients;
    private final PlacementInfo<T> placementInfo;

    public CustomShapelessRecipe(Key id, CraftingRecipeCategory category, String group, List<Ingredient<T>> ingredients, CustomRecipeResult<T> result) {
        super(id, category, group, result);
        this.ingredients = ingredients;
        this.placementInfo = PlacementInfo.create(ingredients);
    }

    public PlacementInfo<T> placementInfo() {
        return placementInfo;
    }

    public List<Ingredient<T>> ingredients() {
        return ingredients;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean matches(RecipeInput input) {
        return matches((CraftingInput<T>) input);
    }

    private boolean matches(CraftingInput<T> input) {
        if (input.ingredientCount() != this.ingredients.size()) {
            return false;
        }
        if (input.size() == 1 && this.ingredients.size() == 1) {
            return this.ingredients.get(0).test(input.getItem(0));
        }
        return input.finder().canCraft(this);
    }

    @Override
    public @NotNull Key type() {
        return RecipeTypes.SHAPELESS;
    }

    public static class Factory<A> implements RecipeFactory<A> {

        @SuppressWarnings({"unchecked", "rawtypes", "DuplicatedCode"})
        @Override
        public Recipe<A> create(Key id, Map<String, Object> arguments) {
            Map<String, Object> ingredientMap = MiscUtils.castToMap(arguments.get("ingredients"), true);
            if (ingredientMap == null) {
                throw new IllegalArgumentException("ingredients cannot be empty");
            }
            CraftingRecipeCategory recipeCategory = arguments.containsKey("category") ? CraftingRecipeCategory.valueOf(arguments.get("category").toString().toUpperCase(Locale.ENGLISH)) : null;
            String group = arguments.containsKey("group") ? arguments.get("group").toString() : null;
            List<Ingredient<A>> ingredients = new ArrayList<>();
            for (Map.Entry<String, Object> entry : ingredientMap.entrySet()) {
                List<String> items = MiscUtils.getAsStringList(entry.getValue());
                Set<Holder<Key>> holders = new HashSet<>();
                for (String item : items) {
                    if (item.charAt(0) == '#') {
                        holders.addAll(CraftEngine.instance().itemManager().tagToItems(Key.of(item.substring(1))));
                    } else {
                        holders.add(BuiltInRegistries.OPTIMIZED_ITEM_ID.get(Key.of(item)).orElseThrow(() -> new IllegalArgumentException("Invalid vanilla/custom item: " + item)));
                    }
                }
                ingredients.add(Ingredient.of(holders));
            }
            return new CustomShapelessRecipe(
                    id,
                    recipeCategory,
                    group,
                    ingredients,
                    parseResult(arguments)
            );
        }
    }
}
