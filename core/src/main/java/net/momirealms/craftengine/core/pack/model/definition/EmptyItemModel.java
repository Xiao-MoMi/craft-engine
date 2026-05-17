package net.momirealms.craftengine.core.pack.model.definition;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.model.generation.ModelGenerationHolder;
import net.momirealms.craftengine.core.pack.revision.Revision;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.util.function.Consumer;

public final class EmptyItemModel implements ItemModel {
    public static final ItemModelFactory<EmptyItemModel> FACTORY = new Factory();
    public static final ItemModelReader<EmptyItemModel> READER = new Reader();
    public static final EmptyItemModel INSTANCE = new EmptyItemModel();
    private static final JsonObject JSON = MiscUtils.init(new JsonObject(), j -> j.addProperty("type", "empty"));

    private EmptyItemModel() {}

    @Override
    public JsonObject toJson(MinecraftVersion min, MinecraftVersion max) {
        return JSON;
    }

    @Override
    public void prepareModelGeneration(Consumer<ModelGenerationHolder> consumer) {
    }

    @Override
    public void gatherRevisions(Consumer<Revision> consumer) {
    }

    private static class Factory implements ItemModelFactory<EmptyItemModel> {
        @Override
        public EmptyItemModel create(ConfigSection section) {
            return INSTANCE;
        }
    }

    private static class Reader implements ItemModelReader<EmptyItemModel> {
        @Override
        public EmptyItemModel read(JsonObject json) {
            return INSTANCE;
        }
    }
}
