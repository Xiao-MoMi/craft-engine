package net.momirealms.craftengine.core.item.recipe.vanilla;

import com.google.gson.JsonObject;

public record RecipeResult(String id, int count, JsonObject components) {

    public boolean isCustom() {
        return components != null;
    }
}
