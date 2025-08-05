package net.momirealms.craftengine.core.item.recipe;

import com.google.gson.JsonObject;

public record DatapackRecipeResult(String id, int count, JsonObject components) {

    public boolean isCustom() {
        return components != null;
    }
}
