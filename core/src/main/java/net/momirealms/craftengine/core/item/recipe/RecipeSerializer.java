package net.momirealms.craftengine.core.item.recipe;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Key;

public interface RecipeSerializer<R extends Recipe> {

    R readConfig(Key id, ConfigSection section);

    R readJson(Key id, JsonObject json);
}
