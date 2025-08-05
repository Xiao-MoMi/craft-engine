package net.momirealms.craftengine.core.item.recipe;

public enum RecipeType {
    CRAFTING("crafting"),
    SMELTING("smelting"),
    BLASTING("blasting"),
    SMOKING("smoking"),
    CAMPFIRE_COOKING("campfire_cooking"),
    STONECUTTING("stonecutting"),
    BREWING("brewing"),
    SMITHING("smithing");

    private final String id;

    RecipeType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
