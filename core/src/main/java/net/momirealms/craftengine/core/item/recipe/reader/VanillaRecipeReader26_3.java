package net.momirealms.craftengine.core.item.recipe.reader;

import com.google.gson.JsonObject;

public class VanillaRecipeReader26_3 extends VanillaRecipeReader26_1 {

    @Override
    public int cookingTime(JsonObject object) {
        if (!object.has("cookingtime")) {
            throw new IllegalArgumentException("Missing required recipe field: cookingtime");
        }
        return object.get("cookingtime").getAsInt();
    }
}
