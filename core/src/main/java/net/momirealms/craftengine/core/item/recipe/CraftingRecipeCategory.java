package net.momirealms.craftengine.core.item.recipe;

public enum CraftingRecipeCategory {
    BUILDING,
    REDSTONE,
    EQUIPMENT,
    MISC;

    public static final CraftingRecipeCategory[] VALUES = CraftingRecipeCategory.values();

    public static CraftingRecipeCategory byId(final int id) {
        return VALUES[id];
    }
}
