package net.momirealms.craftengine.core.pack.model.rangedisptach;

import com.google.gson.JsonObject;

public interface RangeDispatchPropertyReader {
    
    RangeDispatchProperty read(JsonObject json);
}
