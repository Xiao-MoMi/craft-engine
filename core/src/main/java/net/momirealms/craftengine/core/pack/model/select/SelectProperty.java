package net.momirealms.craftengine.core.pack.model.select;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Key;

import java.util.function.Consumer;

public interface SelectProperty extends Consumer<JsonObject> {

    Key type();
}
