package net.momirealms.craftengine.core.pack.model.definition;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.model.generation.ModelGenerationHolder;
import net.momirealms.craftengine.core.pack.revision.Revision;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.MinecraftVersion;

import java.util.function.Consumer;

public final class BundleSelectedItemModel implements ItemModel {
    public static final BundleSelectedItemModel INSTANCE = new BundleSelectedItemModel();
    public static final ItemModelFactory<BundleSelectedItemModel> FACTORY = new Factory();
    public static final ItemModelReader<BundleSelectedItemModel> READER = new Reader();

    private BundleSelectedItemModel() {}

    @Override
    public void prepareModelGeneration(Consumer<ModelGenerationHolder> consumer) {
    }

    @Override
    public void gatherRevisions(Consumer<Revision> consumer) {
    }

    @Override
    public JsonObject toJson(MinecraftVersion min, MinecraftVersion max) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "bundle/selected_item");
        return json;
    }

    private static class Factory implements ItemModelFactory<BundleSelectedItemModel> {
        @Override
        public BundleSelectedItemModel create(ConfigSection section) {
            return INSTANCE;
        }
    }

    private static class Reader implements ItemModelReader<BundleSelectedItemModel> {
        @Override
        public BundleSelectedItemModel read(JsonObject json) {
            return INSTANCE;
        }
    }
}
