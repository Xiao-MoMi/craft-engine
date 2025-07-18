package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.item.recipe.input.CraftingInput;
import net.momirealms.craftengine.core.item.recipe.input.RecipeInput;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.UniqueKey;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CustomShapelessRecipe<T> extends CustomCraftingTableRecipe<T> {
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

    @Override
    public List<Ingredient<T>> ingredientsInUse() {
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

    public static class Factory<A> extends AbstractRecipeFactory<A> {

        @SuppressWarnings({"unchecked", "rawtypes", "DuplicatedCode"})
        @Override
        public Recipe<A> create(Key id, Map<String, Object> arguments) {
            String group = arguments.containsKey("group") ? arguments.get("group").toString() : null;
            List<Ingredient<A>> ingredients = new ArrayList<>();
            Object ingredientsObject = getIngredientOrThrow(arguments);
            if (ingredientsObject instanceof Map<?,?> map) {
                for (Map.Entry<String, Object> entry : (MiscUtils.castToMap(map, false)).entrySet()) {
                    List<String> items = MiscUtils.getAsStringList(entry.getValue());
                    Set<UniqueKey> holders = new HashSet<>();
                    for (String item : items) {
                        if (item.charAt(0) == '#') {
                            holders.addAll(CraftEngine.instance().itemManager().tagToItems(Key.of(item.substring(1))));
                        } else {
                            holders.add(UniqueKey.create(Key.of(item)));
                        }
                    }
                    ingredients.add(Ingredient.of(holders));
                }
            } else if (ingredientsObject instanceof List<?> list) {
                for (Object obj : list) {
                    if (obj instanceof List<?> inner) {
                        Set<UniqueKey> holders = new HashSet<>();
                        for (String item : MiscUtils.getAsStringList(inner)) {
                            if (item.charAt(0) == '#') {
                                holders.addAll(CraftEngine.instance().itemManager().tagToItems(Key.of(item.substring(1))));
                            } else {
                                holders.add(UniqueKey.create(Key.of(item)));
                            }
                        }
                        ingredients.add(Ingredient.of(holders));
                    } else {
                        String item = obj.toString();
                        Set<UniqueKey> holders = new HashSet<>();
                        if (item.charAt(0) == '#') {
                            holders.addAll(CraftEngine.instance().itemManager().tagToItems(Key.of(item.substring(1))));
                        } else {
                            holders.add(UniqueKey.create(Key.of(item)));
                        }
                        ingredients.add(Ingredient.of(holders));
                    }
                }
            } else {
                String item = ingredientsObject.toString();
                Set<UniqueKey> holders = new HashSet<>();
                if (item.charAt(0) == '#') {
                    holders.addAll(CraftEngine.instance().itemManager().tagToItems(Key.of(item.substring(1))));
                } else {
                    holders.add(UniqueKey.create(Key.of(item)));
                }
                ingredients.add(Ingredient.of(holders));
            }
            return new CustomShapelessRecipe(id, craftingRecipeCategory(arguments), group, ingredients, parseResult(arguments));
        }
    }
}
