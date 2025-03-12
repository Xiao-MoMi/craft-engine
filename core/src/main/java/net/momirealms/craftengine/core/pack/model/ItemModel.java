package net.momirealms.craftengine.core.pack.model;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.model.generator.ModelGeneration;
import net.momirealms.craftengine.core.util.Key;

import java.util.List;
import java.util.function.Supplier;

public interface ItemModel extends Supplier<JsonObject> {

    Key type();

    List<ModelGeneration> modelsToGenerate();
}
