package net.momirealms.craftengine.core.pack.model;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.model.generation.ModelGeneration;
import net.momirealms.craftengine.core.pack.revision.Revision;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MinecraftVersion;

import java.util.List;
import java.util.function.Function;

public interface ItemModel extends Function<MinecraftVersion, JsonObject> {

    Key type();

    List<ModelGeneration> modelsToGenerate();

    List<Revision> revisions();
}
