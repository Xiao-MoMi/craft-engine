package net.momirealms.craftengine.core.pack.model.definition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.model.generation.ModelGenerationHolder;
import net.momirealms.craftengine.core.pack.revision.Revision;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class CompositeItemModel implements ItemModel {
    public static final ItemModelFactory<CompositeItemModel> FACTORY = new Factory();
    public static final ItemModelReader<CompositeItemModel> READER = new Reader();
    private final List<ItemModel> models;
    private final Transformation transformation;

    public CompositeItemModel(@NotNull List<ItemModel> models, @Nullable Transformation transformation) {
        this.models = models;
        this.transformation = transformation;
    }

    public CompositeItemModel(@NotNull List<ItemModel> models) {
        this(models, null);
    }

    @Nullable
    public Transformation transformation() {
        return this.transformation;
    }

    @NotNull
    public List<ItemModel> models() {
        return this.models;
    }

    @Override
    public JsonObject toJson(MinecraftVersion min, MinecraftVersion max) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "composite");
        JsonArray array = new JsonArray();
        for (ItemModel model : this.models) {
            array.add(model.toJson(min, max));
        }
        json.add("models", array);
        if (this.transformation != null && max.isAtOrAbove(MinecraftVersion.V26_1)) {
            json.add("transformation", this.transformation.toJson());
        }
        return json;
    }

    @Override
    public void gatherRevisions(Consumer<Revision> consumer) {
        for (ItemModel model : this.models) {
            model.gatherRevisions(consumer);
        }
    }

    @Override
    public void prepareModelGeneration(Consumer<ModelGenerationHolder> consumer) {
        for (ItemModel model : this.models) {
            model.prepareModelGeneration(consumer);
        }
    }

    private static class Factory implements ItemModelFactory<CompositeItemModel> {

        @Override
        public CompositeItemModel create(ConfigSection section) {
            return new CompositeItemModel(
                    section.getList("models", ItemModels::fromConfig),
                    section.getValue("transformation", Transformation::fromConfig)
            );
        }
    }

    private static class Reader implements ItemModelReader<CompositeItemModel> {

        @Override
        public CompositeItemModel read(JsonObject json) {
            JsonArray models = json.getAsJsonArray("models");
            if (models == null) {
                throw new IllegalArgumentException("models is expected to be a JsonArray");
            }
            List<ItemModel> modelList = new ArrayList<>();
            for (JsonElement model : models) {
                if (!(model instanceof JsonObject jo)) {
                    throw new IllegalArgumentException("model is expected to be a JsonObject");
                }
                modelList.add(ItemModels.fromJson(jo));
            }
            return new CompositeItemModel(
                    modelList,
                    json.has("transformation") ? Transformation.fromJson(json.get("transformation")) : null
            );
        }
    }
}
