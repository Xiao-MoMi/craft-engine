package net.momirealms.craftengine.core.pack.model.condition;

import com.google.gson.JsonObject;

public interface ConditionPropertyReader {
    
    ConditionProperty read(JsonObject json);
}
