package net.momirealms.craftengine.core.item.recipe.reader;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.item.recipe.CookingRecipeCategory;
import net.momirealms.craftengine.core.item.recipe.CraftingRecipeCategory;
import net.momirealms.craftengine.core.item.recipe.DatapackRecipeResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public interface VanillaRecipeReader {

    @NotNull DatapackRecipeResult cookingResult(JsonElement object);

    @NotNull DatapackRecipeResult craftingResult(JsonElement object);

    @NotNull DatapackRecipeResult smithingResult(JsonObject object);

    List<List<String>> shapelessIngredients(JsonArray json);

    Map<Character, List<String>> shapedIngredientMap(JsonObject json);

    @NotNull List<String> ingredientList(JsonArray array);

    String[] craftingShapedPattern(JsonObject object);

    @Nullable
    String readGroup(JsonObject object);

    @NotNull
    CraftingRecipeCategory craftingCategory(JsonObject object);

    @NotNull
    CookingRecipeCategory cookingCategory(JsonObject object);

    float cookingExperience(JsonObject object);

    int cookingTime(JsonObject object);

    @NotNull DatapackRecipeResult stoneCuttingResult(JsonObject json);

    List<String> singleIngredient(JsonElement json);

    boolean showNotification(JsonObject json);
}
