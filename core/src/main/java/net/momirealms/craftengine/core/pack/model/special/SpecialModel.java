package net.momirealms.craftengine.core.pack.model.special;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Key;

import java.util.function.Supplier;

public interface SpecialModel extends Supplier<JsonObject> {

    Key type();
}
