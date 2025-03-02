package net.momirealms.craftengine.core.item.recipe.vanilla;

import java.util.List;

public class VanillaStoneCuttingRecipe extends VanillaRecipe {
    private final List<String> ingredient;

    public VanillaStoneCuttingRecipe(String group, RecipeResult result, List<String> ingredient) {
        super(group, result);
        this.ingredient = ingredient;
    }

    public List<String> ingredient() {
        return ingredient;
    }
}
