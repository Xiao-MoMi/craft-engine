package net.momirealms.craftengine.core.pack.model.select;

import com.google.gson.JsonObject;

public interface SelectPropertyReader {
    
    SelectProperty read(JsonObject json);
}
