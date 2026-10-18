package net.momirealms.craftengine.core.pack.model.definition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.pack.model.bbmodel.BBModelConverter;
import net.momirealms.craftengine.core.pack.model.definition.tint.Tint;
import net.momirealms.craftengine.core.pack.model.definition.tint.Tints;
import net.momirealms.craftengine.core.pack.model.generation.ModelGeneration;
import net.momirealms.craftengine.core.pack.model.generation.ModelGenerationHolder;
import net.momirealms.craftengine.core.pack.revision.Revision;
import net.momirealms.craftengine.core.pack.revision.Revisions;
import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class BaseItemModel implements ItemModel {
    public static final ItemModelFactory<BaseItemModel> FACTORY = new Factory();
    public static final ItemModelReader<BaseItemModel> READER = new Reader();
    private final Key path;
    private final List<Tint> tints;
    private final Transformation transformation;

    public BaseItemModel(@NotNull Key path,
                         @Nullable List<Tint> tints,
                         @Nullable Transformation transformation) {
        this.path = path;
        this.tints = tints;
        this.transformation = transformation;
    }

    public BaseItemModel(@NotNull Key path) {
        this(path, List.of(), null);
    }

    public BaseItemModel(@NotNull Key path,
                         @NotNull List<Tint> tints) {
        this(path, tints, null);
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
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof BaseItemModel that)) {
            return false;
        }
        return this.path.equals(that.path)
                && Objects.equals(this.tints, that.tints)
                && Objects.equals(this.transformation, that.transformation);
    }

    @Override
    public int hashCode() {
        int result = this.path.hashCode();
        result = 31 * result + Objects.hashCode(this.tints);
        return 31 * result + Objects.hashCode(this.transformation);
    }

    public static BaseItemModel ofBBModel(BBModelConverter.Converted converted, Consumer<ModelGenerationHolder> generations) {
        return ofBBModel(converted, List.of(), null, generations);
    }

    public static BaseItemModel ofBBModel(BBModelConverter.Converted converted, @Nullable List<Tint> tints, @Nullable Transformation transformation, Consumer<ModelGenerationHolder> generations) {
        generations.accept(new ModelGenerationHolder(converted.model(), ModelGeneration.raw(converted.json(), converted.textures(), converted.textureMetadata())));
        return new BaseItemModel(converted.model(), tints, transformation);
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
    public void gatherRevisions(Consumer<Revision> consumer) {
        if (this.transformation != null) {
            consumer.accept(Revisions.SINCE_26_1);
        }
    }

    private static class Factory implements ItemModelFactory<BaseItemModel> {
        private static final String[] PATH = ConfigKeys.of("path|model");

        @Override
        public BaseItemModel create(Pack pack, Path path, ConfigSection section, Consumer<ModelGenerationHolder> generations) {
            List<Tint> tints = section.getList("tints", Tints::fromConfig);
            Transformation transformation = section.getValue("transformation", Transformation::fromConfig);
            ConfigValue blueprintValue = section.getValue("blueprint");
            if (blueprintValue != null) {
                return BaseItemModel.ofBBModel(BBModelConverter.convert(pack, path, "item", section.getValue(PATH), blueprintValue), tints, transformation, generations);
            }
            Key modelPath = section.getNonNullAssetPath(PATH);
            ConfigSection generation = section.getSection("generation");
            if (generation != null) {
                generations.accept(new ModelGenerationHolder(modelPath, ModelGeneration.of(generation)));
            }
            return new BaseItemModel(
                    modelPath,
                    tints,
                    transformation
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
                    json.has("transformation") ? Transformation.fromJson(json.get("transformation")) : null
            );
        }
    }
}
