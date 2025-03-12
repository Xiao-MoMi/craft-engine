package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.VersionHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RecipeUtils {

    private RecipeUtils() {}

    @SuppressWarnings("unchecked")
    public static List<Object> getIngredientsFromShapedRecipe(Object recipe) {
        List<Object> ingredients = new ArrayList<>();
        try {
            if (VersionHelper.isVersionNewerThan1_20_3()) {
                Object pattern = Reflections.field$1_20_3$ShapedRecipe$pattern.get(recipe);
                if (VersionHelper.isVersionNewerThan1_21_2()) {
                    List<Optional<Object>> optionals = (List<Optional<Object>>) Reflections.field$ShapedRecipePattern$ingredients1_21_2.get(pattern);
                    for (Optional<Object> optional : optionals) {
                        optional.ifPresent(ingredients::add);
                    }
                } else {
                    List<Object> objectList = (List<Object>) Reflections.field$ShapedRecipePattern$ingredients1_20_3.get(pattern);
                    for (Object object : objectList) {
                        Object[] values = (Object[]) Reflections.field$Ingredient$values.get(object);
                        // is empty or not
                        if (values.length != 0) {
                            ingredients.add(object);
                        }
                    }
                }
            } else {
                List<Object> objectList = (List<Object>) Reflections.field$1_20_1$ShapedRecipe$recipeItems.get(recipe);
                for (Object object : objectList) {
                    Object[] values = (Object[]) Reflections.field$Ingredient$values.get(object);
                    if (values.length != 0) {
                        ingredients.add(object);
                    }
                }
            }
        } catch (ReflectiveOperationException e) {
            CraftEngine.instance().logger().warn("Failed to get ingredients from shaped recipe", e);
        }
        return ingredients;
    }
}
