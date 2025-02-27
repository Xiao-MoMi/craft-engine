package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.item.recipe.input.RecipeInput;
import net.momirealms.craftengine.core.item.recipe.input.SingleItemInput;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CustomStoneCuttingRecipe<T> extends AbstractRecipe<T> {
    public static final Factory<?> FACTORY = new Factory<>();
    protected final Ingredient<T> ingredient;

    protected CustomStoneCuttingRecipe(Key id, String group, Ingredient<T> ingredient, CustomRecipeResult<T> result) {
        super(id, group, result);
        this.ingredient = ingredient;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean matches(RecipeInput input) {
        return this.ingredient.test(((SingleItemInput<T>) input).input());
    }

    @Override
    public @NotNull Key type() {
        return RecipeTypes.STONE_CUTTING;
    }

    public Ingredient<T> ingredient() {
        return ingredient;
    }

    public static class Factory<A> implements RecipeFactory<A> {

        @SuppressWarnings({"DuplicatedCode"})
        @Override
        public Recipe<A> create(Key id, Map<String, Object> arguments) {
            String group = arguments.containsKey("group") ? arguments.get("group").toString() : null;
            List<String> items = MiscUtils.getAsStringList(arguments.get("ingredient"));
            Set<Holder<Key>> holders = new HashSet<>();
            for (String item : items) {
                if (item.charAt(0) == '#') {
                    holders.addAll(CraftEngine.instance().itemManager().tagToItems(Key.of(item.substring(1))));
                } else {
                    holders.add(BuiltInRegistries.OPTIMIZED_ITEM_ID.get(Key.of(item)).orElseThrow(() -> new IllegalArgumentException("Invalid vanilla/custom item: " + item)));
                }
            }
            return new CustomStoneCuttingRecipe<>(
                    id,
                    group,
                    Ingredient.of(holders),
                    parseResult(arguments)
            );
        }
    }
}
