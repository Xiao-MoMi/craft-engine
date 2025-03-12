package net.momirealms.craftengine.core.pack.model.condition;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Key;

import java.util.function.Consumer;

public interface ConditionProperty extends Consumer<JsonObject> {

    Key type();
}
