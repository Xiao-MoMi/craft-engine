package net.momirealms.craftengine.core.item.recipe;

public enum CookingRecipeCategory {
    FOOD,
    BLOCKS,
    MISC;

    public static final CookingRecipeCategory[] VALUES = CookingRecipeCategory.values();

    public static CookingRecipeCategory byId(final int id) {
        return VALUES[id];
    }
}
