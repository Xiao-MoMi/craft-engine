package net.momirealms.craftengine.core.pack.model.special;

import com.google.gson.JsonObject;

public interface SpecialModelReader {

    SpecialModel read(JsonObject json);
}
