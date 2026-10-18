package net.momirealms.craftengine.core.pack.model.definition;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.revision.Revision;
import net.momirealms.craftengine.core.util.MinecraftVersion;

import java.util.function.Consumer;

public interface ItemModel {

    void gatherRevisions(Consumer<Revision> consumer);

    JsonObject toJson(MinecraftVersion min, MinecraftVersion max);
}
