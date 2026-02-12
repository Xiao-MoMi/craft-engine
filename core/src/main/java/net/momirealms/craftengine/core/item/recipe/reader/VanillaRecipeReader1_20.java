package net.momirealms.craftengine.core.item.recipe.reader;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.item.recipe.CookingRecipeCategory;
import net.momirealms.craftengine.core.item.recipe.CraftingRecipeCategory;
import net.momirealms.craftengine.core.item.recipe.DatapackRecipeResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class VanillaRecipeReader1_20 implements VanillaRecipeReader {

    @Override
    public @NotNull DatapackRecipeResult cookingResult(JsonElement object) {
        return new DatapackRecipeResult(object.getAsString(), 1, null);
    }

    @Override
    public @NotNull DatapackRecipeResult craftingResult(JsonElement je) {
        JsonObject object = je.getAsJsonObject();
        String item = object.get("item").getAsString();
        int count = object.has("count") ? object.get("count").getAsInt() : 1;
        return new DatapackRecipeResult(item, count, null);
    }

    @Override
    public @NotNull DatapackRecipeResult smithingResult(JsonObject object) {
        String item = object.get("item").getAsString();
        return new DatapackRecipeResult(item, 1, null);
    }

    @Override
    public List<List<String>> shapelessIngredients(JsonArray json) {
        List<List<String>> ingredients = new ArrayList<>();
        for (JsonElement element : json) {
            if (element.isJsonObject()) {
                JsonObject jsonObject = element.getAsJsonObject();
                if (jsonObject.has("item")) {
                    ingredients.add(List.of(jsonObject.get("item").getAsString()));
                } else if (jsonObject.has("tag")) {
                    ingredients.add(List.of("#" + jsonObject.get("tag").getAsString()));
                }
            } else if (element.isJsonArray()) {
                List<String> ingredient = ingredientList((JsonArray) element);
                ingredients.add(ingredient);
            }
        }
        return ingredients;
    }

    @Override
    public Map<Character, List<String>> shapedIngredientMap(JsonObject json) {
        Map<Character, List<String>> ingredients = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            char c = entry.getKey().charAt(0);
            if (entry.getValue().isJsonObject()) {
                JsonObject argument = entry.getValue().getAsJsonObject();
                if (argument.has("item")) {
                    ingredients.put(c, List.of(argument.get("item").getAsString()));
                } else if (argument.has("tag")) {
                    ingredients.put(c, List.of("#" + argument.get("tag").getAsString()));
                }
            } else if (entry.getValue().isJsonArray()) {
                List<String> items = ingredientList((JsonArray) entry.getValue());
                ingredients.put(c, items);
            }
        }
        return ingredients;
    }

    @Override
    public @NotNull List<String> ingredientList(JsonArray array) {
        List<String> items = new ArrayList<>();
        for (JsonElement element : array) {
            if (element.isJsonObject()) {
                JsonObject argument = element.getAsJsonObject();
                if (argument.has("item")) {
                    items.add(argument.get("item").getAsString());
                } else if (argument.has("tag")) {
                    items.add("#" + argument.get("tag").getAsString());
                }
            }
        }
        return items;
    }

    @Override
    public String[] craftingShapedPattern(JsonObject object) {
        JsonArray pattern = object.getAsJsonArray("pattern");
        List<String> patternList = new ArrayList<>();
        for (JsonElement element : pattern) {
            patternList.add(element.getAsString());
        }
        return patternList.toArray(new String[0]);
    }

    @Override
    public @Nullable String readGroup(JsonObject object) {
        return object.has("group") ? object.get("group").getAsString() : null;
    }

    @Override
    public @NotNull CraftingRecipeCategory craftingCategory(JsonObject object) {
        return object.has("category") ? CraftingRecipeCategory.valueOf(object.get("category").getAsString().toUpperCase(Locale.ENGLISH)) : CraftingRecipeCategory.MISC;
    }

    @Override
    public @NotNull CookingRecipeCategory cookingCategory(JsonObject object) {
        return object.has("category") ? CookingRecipeCategory.valueOf(object.get("category").getAsString().toUpperCase(Locale.ENGLISH)) : CookingRecipeCategory.MISC;
    }

    @Override
    public float cookingExperience(JsonObject object) {
        return object.has("experience") ? object.get("experience").getAsFloat() : 0;
    }

    @Override
    public int cookingTime(JsonObject object) {
        return object.has("cookingtime") ? object.get("cookingtime").getAsInt() : 200;
    }

    @Override
    public @NotNull DatapackRecipeResult stoneCuttingResult(JsonObject json) {
        int count = json.has("count") ? json.get("count").getAsInt() : 1;
        String result = json.get("result").getAsString();
        return new DatapackRecipeResult(result, count, null);
    }

    @Override
    public List<String> singleIngredient(JsonElement json) {
        List<String> ingredients = new ArrayList<>();
        if (json.isJsonObject()) {
            JsonObject argument = json.getAsJsonObject();
            if (argument.has("item")) {
                ingredients.add(argument.get("item").getAsString());
            } else if (argument.has("tag")) {
                ingredients.add("#" + argument.get("tag").getAsString());
            }
        } else if (json.isJsonArray()) {
            List<String> items = ingredientList((JsonArray) json);
            ingredients.addAll(items);
        }
        return ingredients;
    }

    @Override
    public boolean showNotification(JsonObject json) {
        return !json.has("show_notification") || json.get("show_notification").getAsBoolean();
    }
}
