package net.momirealms.craftengine.core.item.recipe.reader;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.item.recipe.DatapackRecipeResult;
import org.jetbrains.annotations.NotNull;

public class VanillaRecipeReader1_20_5 extends VanillaRecipeReader1_20 {

    @Override
    public @NotNull DatapackRecipeResult craftingResult(JsonElement je) {
        JsonObject object = je.getAsJsonObject();
        String item = object.get("id").getAsString();
        JsonObject components = object.has("components") ? object.getAsJsonObject("components") : null;
        int count = object.has("count") ? object.get("count").getAsInt() : 1;
        return new DatapackRecipeResult(item, count, components);
    }

    @NotNull
    @Override
    public DatapackRecipeResult cookingResult(JsonElement object) {
        return craftingResult(object.getAsJsonObject());
    }

    @NotNull
    @Override
    public DatapackRecipeResult stoneCuttingResult(JsonObject json) {
        return craftingResult(json.getAsJsonObject("result"));
    }

    @Override
    public @NotNull DatapackRecipeResult smithingResult(JsonObject object) {
        return craftingResult(object.getAsJsonObject());
    }
}
