package net.momirealms.craftengine.core.pack.model;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.model.generation.ModelGeneration;
import net.momirealms.craftengine.core.pack.revision.Revision;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MinecraftVersion;

import java.util.List;
import java.util.Map;

public class EmptyItemModel implements ItemModel {
    public static final Factory FACTORY = new Factory();
    public static final Reader READER = new Reader();
    private static final EmptyItemModel INSTANCE = new EmptyItemModel();

    @Override
    public JsonObject apply(MinecraftVersion version) {
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

    @Override
    public List<Revision> revisions() {
        return List.of();
    }

    public static class Factory implements ItemModelFactory {
        @Override
        public ItemModel create(Map<String, Object> arguments) {
            return INSTANCE;
        }
    }

    public static class Reader implements ItemModelReader {
        @Override
        public ItemModel read(JsonObject json) {
            return INSTANCE;
        }
    }
}
