package net.momirealms.craftengine.core.pack.model.definition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.model.definition.tint.Tint;
import net.momirealms.craftengine.core.pack.model.definition.tint.Tints;
import net.momirealms.craftengine.core.pack.model.generation.ModelGeneration;
import net.momirealms.craftengine.core.pack.model.generation.ModelGenerationHolder;
import net.momirealms.craftengine.core.pack.revision.Revision;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public final class BaseItemModel implements ItemModel {
    public static final ItemModelFactory<BaseItemModel> FACTORY = new Factory();
    public static final ItemModelReader<BaseItemModel> READER = new Reader();
    private final Key path;
    private final List<Tint> tints;
    private final ModelGeneration modelGeneration;
    private final Transformation transformation;

    public BaseItemModel(@NotNull Key path,
                         @Nullable List<Tint> tints,
                         @Nullable ModelGeneration modelGeneration,
                         @Nullable Transformation transformation) {
        this.path = path;
        this.tints = tints;
        this.modelGeneration = modelGeneration;
        this.transformation = transformation;
    }

    public BaseItemModel(@NotNull Key path) {
        this(path, List.of(), null, null);
    }

    public BaseItemModel(@NotNull Key path,
                         @NotNull List<Tint> tints) {
        this(path, tints, null, null);
    }

    public BaseItemModel(@NotNull Key path,
                         @NotNull List<Tint> tints,
                         @Nullable ModelGeneration modelGeneration) {
        this(path, tints, modelGeneration, null);
    }

    @Nullable
    public ModelGeneration modelGeneration() {
        return this.modelGeneration;
    }

    @Nullable
    public List<Tint> tints() {
        return this.tints;
    }

    @NotNull
    public Key path() {
        return this.path;
    }

    @Nullable
    public Transformation transformation() {
        return this.transformation;
    }

    @Override
    public JsonObject toJson(MinecraftVersion min, MinecraftVersion max) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "model");
        json.addProperty("model", this.path.asMinimalString());
        if (this.tints != null && !this.tints.isEmpty()) {
            JsonArray array = new JsonArray();
            for (Tint tint : this.tints) {
                array.add(tint.get());
            }
            json.add("tints", array);
        }
        if (this.transformation != null && max.isAtOrAbove(MinecraftVersion.V26_1)) {
            json.add("transformation", this.transformation.toJson());
        }
        return json;
    }

    @Override
    public void prepareModelGeneration(Consumer<ModelGenerationHolder> consumer) {
        if (this.modelGeneration != null) {
            consumer.accept(new ModelGenerationHolder(this.path, this.modelGeneration));
        }
    }

    @Override
    public void gatherRevisions(Consumer<Revision> consumer) {
    }

    private static class Factory implements ItemModelFactory<BaseItemModel> {
        private static final String[] PATH = new String[] {"path", "model"};

        @Override
        public BaseItemModel create(ConfigSection section) {
            Key modelPath = section.getNonNullIdentifier(PATH);
            ConfigSection generation = section.getSection("generation");
            ModelGeneration modelGeneration = null;
            if (generation != null) {
                modelGeneration = ModelGeneration.of(generation);
            }
            return new BaseItemModel(
                    modelPath,
                    section.getList("tints", Tints::fromConfig),
                    modelGeneration,
                    section.getValue("transformation", Transformation::fromConfig)
            );
        }
    }

    private static class Reader implements ItemModelReader<BaseItemModel> {

        @Override
        public BaseItemModel read(JsonObject json) {
            String model = json.get("model").getAsString();
            List<Tint> tints;
            if (json.has("tints")) {
                JsonArray array = json.getAsJsonArray("tints");
                tints = new ArrayList<>(array.size());
                for (JsonElement element : array) {
                    if (element instanceof JsonObject jo) {
                        tints.add(Tints.fromJson(jo));
                    } else {
                        throw new IllegalArgumentException("tint is expected to be a json object");
                    }
                }
            } else {
                tints = Collections.emptyList();
            }
            return new BaseItemModel(
                    Key.of(model),
                    tints,
                    null,
                    json.has("transformation") ? Transformation.fromJson(json.get("transformation")) : null
            );
        }
    }
}
