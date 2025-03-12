package net.momirealms.craftengine.core.pack.model;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.model.generator.ModelGeneration;
import net.momirealms.craftengine.core.util.Key;

import java.util.List;
import java.util.Map;

public class EmptyItemModel implements ItemModel {
    public static final Factory FACTORY = new Factory();
    private static final EmptyItemModel INSTANCE = new EmptyItemModel();

    @Override
    public JsonObject get() {
        JsonObject json = new JsonObject();
        json.addProperty("type", type().toString());
        return json;
    }

    @Override
    public Key type() {
        return ItemModels.EMPTY;
    }

    @Override
    public List<ModelGeneration> modelsToGenerate() {
        return List.of();
    }

    public static class Factory implements ItemModelFactory {

        @Override
        public ItemModel create(Map<String, Object> arguments) {
            return INSTANCE;
        }
    }
}
