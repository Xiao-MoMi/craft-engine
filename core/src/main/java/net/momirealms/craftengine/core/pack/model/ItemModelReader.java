package net.momirealms.craftengine.core.pack.model;

import com.google.gson.JsonObject;

public interface ItemModelReader {

    ItemModel read(JsonObject json);
}
