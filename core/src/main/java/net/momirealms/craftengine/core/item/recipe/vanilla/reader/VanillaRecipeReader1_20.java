package net.momirealms.craftengine.core.item.recipe.vanilla.reader;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.item.recipe.vanilla.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VanillaRecipeReader1_20 extends AbstractRecipeReader {

    @Override
    public VanillaShapedRecipe readShaped(JsonObject json) {
        return new VanillaShapedRecipe(
                readCraftingCategory(json),
                readGroup(json),
                readShapedIngredientMap(json.getAsJsonObject("key")),
                readPattern(json),
                readCraftingResult(json.getAsJsonObject("result"))
        );
    }

    @Override
    public VanillaShapelessRecipe readShapeless(JsonObject json) {
        return new VanillaShapelessRecipe(
                readCraftingCategory(json),
                readGroup(json),
                readShapelessIngredients(json.getAsJsonArray("ingredients")),
                readCraftingResult(json.getAsJsonObject("result"))
        );
    }

    @Override
    public VanillaBlastingRecipe readBlasting(JsonObject json) {
        return new VanillaBlastingRecipe(
                readCookingCategory(json),
                readGroup(json),
                readCookingResult(json.get("result")),
                readSingleIngredient(json.get("ingredient")),
                readExperience(json),
                readCookingTime(json)
        );
    }

    @Override
    public VanillaSmeltingRecipe readSmelting(JsonObject json) {
        return new VanillaSmeltingRecipe(
                readCookingCategory(json),
                readGroup(json),
                readCookingResult(json.get("result")),
                readSingleIngredient(json.get("ingredient")),
                readExperience(json),
                readCookingTime(json)
        );
    }

    @Override
    public VanillaSmokingRecipe readSmoking(JsonObject json) {
        return new VanillaSmokingRecipe(
                readCookingCategory(json),
                readGroup(json),
                readCookingResult(json.get("result")),
                readSingleIngredient(json.get("ingredient")),
                readExperience(json),
                readCookingTime(json)
        );
    }

    @Override
    public VanillaCampfireRecipe readCampfire(JsonObject json) {
        return new VanillaCampfireRecipe(
                readCookingCategory(json),
                readGroup(json),
                readCookingResult(json.get("result")),
                readSingleIngredient(json.get("ingredient")),
                readExperience(json),
                readCookingTime(json)
        );
    }

    @Override
    public VanillaStoneCuttingRecipe readStoneCutting(JsonObject json) {
        return new VanillaStoneCuttingRecipe(
                readGroup(json),
                readStoneCuttingResult(json),
                readSingleIngredient(json.get("ingredient"))
        );
    }

    protected List<String> readSingleIngredient(JsonElement json) {
        List<String> ingredients = new ArrayList<>();
        if (json.isJsonObject()) {
            JsonObject argument = json.getAsJsonObject();
            if (argument.has("item")) {
                ingredients.add(argument.get("item").getAsString());
            } else if (argument.has("tag")) {
                ingredients.add("#" + argument.get("tag").getAsString());
            }
        } else if (json.isJsonArray()) {
            List<String> items = readIngredientList((JsonArray) json);
            ingredients.addAll(items);
        }
        return ingredients;
    }

    @NotNull
    protected RecipeResult readStoneCuttingResult(JsonObject json) {
        int count = json.has("count") ? json.get("count").getAsInt() : 1;
        String result = json.get("result").getAsString();
        return new RecipeResult(result, count, null);
    }

    @NotNull
    protected RecipeResult readCookingResult(JsonElement object) {
        return new RecipeResult(object.getAsString(), 1, null);
    }

    @NotNull
    protected RecipeResult readCraftingResult(JsonObject object) {
        String item = object.get("item").getAsString();
        int count = object.has("count") ? object.get("count").getAsInt() : 1;
        return new RecipeResult(item, count, null);
    }

    protected List<List<String>> readShapelessIngredients(JsonArray json) {
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
                List<String> ingredient = readIngredientList((JsonArray) element);
                ingredients.add(ingredient);
            }
        }
        return ingredients;
    }

    protected Map<Character, List<String>> readShapedIngredientMap(JsonObject json) {
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
                List<String> items = readIngredientList((JsonArray) entry.getValue());
                ingredients.put(c, items);
            }
        }
        return ingredients;
    }

    protected @NotNull List<String> readIngredientList(JsonArray array) {
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
}
