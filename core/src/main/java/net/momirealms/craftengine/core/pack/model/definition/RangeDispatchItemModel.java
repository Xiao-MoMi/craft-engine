package net.momirealms.craftengine.core.pack.model.definition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.model.definition.rangedisptach.RangeDispatchProperties;
import net.momirealms.craftengine.core.pack.model.definition.rangedisptach.RangeDispatchProperty;
import net.momirealms.craftengine.core.pack.model.generation.ModelGenerationHolder;
import net.momirealms.craftengine.core.pack.revision.Revision;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

public final class RangeDispatchItemModel implements ItemModel {
    public static final ItemModelFactory<RangeDispatchItemModel> FACTORY = new Factory();
    public static final ItemModelReader<RangeDispatchItemModel> READER = new Reader();
    private final RangeDispatchProperty property;
    private final float scale;
    private final ItemModel fallBack;
    private final Map<Float, ItemModel> entries;
    private final Transformation transformation;

    public RangeDispatchItemModel(@NotNull RangeDispatchProperty property,
                                  float scale,
                                  @NotNull Map<Float, ItemModel> entries,
                                  @Nullable ItemModel fallBack,
                                  @Nullable Transformation transformation) {
        this.property = property;
        this.scale = scale;
        this.fallBack = fallBack;
        this.entries = entries;
        this.transformation = transformation;
    }

    public RangeDispatchItemModel(@NotNull RangeDispatchProperty property,
                                  float scale,
                                  @NotNull Map<Float, ItemModel> entries,
                                  @Nullable ItemModel fallBack) {
        this(property, scale, entries, fallBack, null);
    }

    @Nullable
    public Transformation transformation() {
        return this.transformation;
    }

    @NotNull
    public RangeDispatchProperty property() {
        return this.property;
    }

    public float scale() {
        return this.scale;
    }

    @Nullable
    public ItemModel fallBack() {
        return this.fallBack;
    }

    @NotNull
    public Map<Float, ItemModel> entries() {
        return this.entries;
    }

    @Override
    public JsonObject toJson(MinecraftVersion min, MinecraftVersion max) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "range_dispatch");
        this.property.writeProperty(json);
        JsonArray array = new JsonArray();
        for (Map.Entry<Float, ItemModel> entry : this.entries.entrySet()) {
            float threshold = entry.getKey();
            ItemModel model = entry.getValue();
            JsonObject jo = new JsonObject();
            jo.addProperty("threshold", threshold);
            jo.add("model", model.toJson(min, max));
            array.add(jo);
        }
        json.add("entries", array);
        if (this.scale != 1) {
            json.addProperty("scale", this.scale);
        }
        if (this.fallBack != null) {
            json.add("fallback", this.fallBack.toJson(min, max));
        }
        if (this.transformation != null && max.isAtOrAbove(MinecraftVersion.V26_1)) {
            json.add("transformation", this.transformation.toJson());
        }
        return json;
    }

    @Override
    public void gatherRevisions(Consumer<Revision> consumer) {
        if (this.fallBack != null) {
            this.fallBack.gatherRevisions(consumer);
        }
        for (ItemModel model : this.entries.values()) {
            model.gatherRevisions(consumer);
        }
    }

    @Override
    public void prepareModelGeneration(Consumer<ModelGenerationHolder> consumer) {
        if (this.fallBack != null) {
            this.fallBack.prepareModelGeneration(consumer);
        }
        for (ItemModel model : this.entries.values()) {
            model.prepareModelGeneration(consumer);
        }
    }

    private static class Factory implements ItemModelFactory<RangeDispatchItemModel> {

        @Override
        public RangeDispatchItemModel create(ConfigSection section) {
            RangeDispatchProperty property = RangeDispatchProperties.fromConfig(section);
            float scale = section.getFloat("scale", 1.0f);
            ItemModel fallbackModel = section.getValue("fallback", ItemModels::fromConfig);
            Map<Float, ItemModel> entryMap = new TreeMap<>();
            ConfigValue entries = section.getNonNullValue("entries", ConfigConstants.ARGUMENT_LIST);
            entries.forEach(value -> {
                ConfigSection entry = value.getAsSection();
                float threshold = entry.getNonNullFloat("threshold");
                ItemModel model = entry.getValue("model", ItemModels::fromConfig, fallbackModel);
                entryMap.put(threshold, model);
            });
            return new RangeDispatchItemModel(
                    property,
                    scale,
                    entryMap,
                    fallbackModel,
                    section.getValue("transformation", Transformation::fromConfig)
            );
        }
    }

    private static class Reader implements ItemModelReader<RangeDispatchItemModel> {

        @Override
        public RangeDispatchItemModel read(JsonObject json) {
            JsonArray entriesArray = json.getAsJsonArray("entries");
            if (entriesArray == null) {
                throw new IllegalArgumentException("entries is expected to be a JsonArray");
            }
            Map<Float, ItemModel> entries = new TreeMap<>();
            for (JsonElement entry : entriesArray) {
                if (entry instanceof JsonObject entryObj) {
                    float threshold = entryObj.getAsJsonPrimitive("threshold").getAsFloat();
                    ItemModel model = ItemModels.fromJson(entryObj.getAsJsonObject("model"));
                    entries.put(threshold, model);
                }
            }
            return new RangeDispatchItemModel(RangeDispatchProperties.fromJson(json),
                    json.has("scale") ? json.get("scale").getAsFloat() : 1f,
                    entries,
                    json.has("fallback") ? ItemModels.fromJson(json.getAsJsonObject("fallback")) : null,
                    json.has("transformation") ? Transformation.fromJson(json.get("transformation")) : null
            );
        }
    }
}
