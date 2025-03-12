package net.momirealms.craftengine.core.pack.model.rangedisptach;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Key;

import java.util.function.Consumer;

public interface RangeDispatchProperty extends Consumer<JsonObject> {

    Key type();
}
