package net.momirealms.craftengine.core.pack.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.ResourceLocation;
import net.momirealms.craftengine.core.pack.model.generation.ModelGeneration;
import net.momirealms.craftengine.core.pack.model.tint.Tint;
import net.momirealms.craftengine.core.pack.model.tint.Tints;
import net.momirealms.craftengine.core.pack.revision.Revision;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BaseItemModel implements ItemModel {
    public static final Factory FACTORY = new Factory();
    public static final Reader READER = new Reader();
    private final String path;
    private final List<Tint> tints;
    private final ModelGeneration modelGeneration;

    public BaseItemModel(String path, List<Tint> tints, @Nullable ModelGeneration modelGeneration) {
        this.path = path;
        this.tints = tints;
        this.modelGeneration = modelGeneration;
    }

    public List<Tint> tints() {
        return this.tints;
    }

    public String path() {
        return this.path;
    }

    @Override
    public JsonObject apply(MinecraftVersion version) {
        JsonObject json = new JsonObject();
        json.addProperty("type", type().toString());
        json.addProperty("model", this.path);
        if (!this.tints.isEmpty()) {
            JsonArray array = new JsonArray();
            for (Tint tint : this.tints) {
                array.add(tint.get());
            }
            json.add("tints", array);
        }
        return json;
    }

    @Override
    public Key type() {
        return ItemModels.MODEL;
    }

    @Override
    public List<ModelGeneration> modelsToGenerate() {
        if (this.modelGeneration == null) {
            return List.of();
        } else {
            return List.of(this.modelGeneration);
        }
    }

    @Override
    public List<Revision> revisions() {
        return List.of();
    }

    public static class Factory implements ItemModelFactory {

        @SuppressWarnings("unchecked")
        @Override
        public ItemModel create(Map<String, Object> arguments) {
            String modelPath = ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("path"), "warning.config.item.model.base.missing_path");
            if (!ResourceLocation.isValid(modelPath)) {
                throw new LocalizedResourceConfigException("warning.config.item.model.base.invalid_path", modelPath);
            }
            Map<String, Object> generation = MiscUtils.castToMap(arguments.get("generation"), true);
            ModelGeneration modelGeneration = null;
            if (generation != null) {
                modelGeneration = ModelGeneration.of(Key.of(modelPath), generation);
            }
            if (arguments.containsKey("tints")) {
                List<Tint> tints = new ArrayList<>();
                List<Map<String, Object>> tintList = (List<Map<String, Object>>) arguments.get("tints");
                for (Map<String, Object> tint : tintList) {
                    tints.add(Tints.fromMap(tint));
                }
                return new BaseItemModel(modelPath, tints, modelGeneration);
            } else {
                return new BaseItemModel(modelPath, List.of(), modelGeneration);
            }
        }
    }

    public static class Reader implements ItemModelReader {

        @Override
        public ItemModel read(JsonObject json) {
            String model = json.get("model").getAsString();
            if (json.has("tints")) {
                JsonArray array = json.getAsJsonArray("tints");
                List<Tint> tints = new ArrayList<>(array.size());
                for (JsonElement element : array) {
                    if (element instanceof JsonObject jo) {
                        tints.add(Tints.fromJson(jo));
                    } else {
                        throw new IllegalArgumentException("tint is expected to be a json object");
                    }
                }
                return new BaseItemModel(model, tints, null);
            } else {
                return new BaseItemModel(model, List.of(), null);
            }
        }
    }
}
