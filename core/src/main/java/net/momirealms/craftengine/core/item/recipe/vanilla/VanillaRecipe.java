package net.momirealms.craftengine.core.item.recipe.vanilla;

public abstract class VanillaRecipe {
    protected final String group;
    protected final RecipeResult result;

    protected VanillaRecipe(String group, RecipeResult result) {
        this.group = group;
        this.result = result;
    }

    public String group() {
        return group;
    }

    public RecipeResult result() {
        return result;
    }
}
