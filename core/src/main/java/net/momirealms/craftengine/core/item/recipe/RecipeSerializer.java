package net.momirealms.craftengine.core.item.recipe;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Key;

import java.util.Map;

public interface RecipeSerializer<T, R extends Recipe<T>> {

    R readMap(Key id, Map<String, Object> arguments);

    R readJson(Key id, JsonObject json);
}
