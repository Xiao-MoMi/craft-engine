package net.momirealms.craftengine.core.pack.model.definition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.pack.model.definition.rangedisptach.RangeDispatchProperties;
import net.momirealms.craftengine.core.pack.model.definition.rangedisptach.RangeDispatchProperty;
import net.momirealms.craftengine.core.pack.model.generation.ModelGenerationHolder;
import net.momirealms.craftengine.core.pack.revision.Revision;
import net.momirealms.craftengine.core.pack.revision.Revisions;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

public final class RangeDispatchItemModel implements ItemModel {
    public static final ItemModelFactory<RangeDispatchItemModel> FACTORY = new Factory();
    public static final ItemModelReader<RangeDispatchItemModel> READER = new Reader();
    private final RangeDispatchProperty property;
    private final float scale;
    private final ItemModel fallBack;
    private final List<Entry> entries;
    private final Transformation transformation;

    public RangeDispatchItemModel(@NotNull RangeDispatchProperty property,
                                  float scale,
                                  @NotNull Map<Float, ItemModel> entries,
                                  @Nullable ItemModel fallBack,
                                  @Nullable Transformation transformation) {
        this(property, scale, entries.entrySet().stream().map(entry -> new Entry(entry.getKey(), entry.getValue())).toList(), fallBack, transformation);
    }

    public RangeDispatchItemModel(RangeDispatchProperty property, float scale, List<Entry> entries,
                                  @Nullable ItemModel fallBack, @Nullable Transformation transformation) {
        this.property = property;
        this.scale = scale;
        this.fallBack = fallBack;
        this.entries = List.copyOf(entries);
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
    public List<Entry> entries() {
        return this.entries;
    }

    @Override
    public JsonObject toJson(MinecraftVersion min, MinecraftVersion max) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "range_dispatch");
        this.property.writeProperty(json);
        JsonArray array = new JsonArray();
        for (Entry entry : this.entries) {
            float threshold = entry.threshold();
            ItemModel model = entry.model();
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
        if (this.transformation != null) {
            consumer.accept(Revisions.SINCE_26_1);
        }
        for (Entry entry : this.entries) {
            entry.model().gatherRevisions(consumer);
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof RangeDispatchItemModel that)) {
            return false;
        }
        return Objects.equals(this.property, that.property)
                && Float.compare(this.scale, that.scale) == 0
                && Objects.equals(this.fallBack, that.fallBack)
                && Objects.equals(this.entries, that.entries)
                && Objects.equals(this.transformation, that.transformation);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(this.property);
        result = 31 * result + Float.hashCode(this.scale);
        result = 31 * result + Objects.hashCode(this.fallBack);
        result = 31 * result + Objects.hashCode(this.entries);
        return 31 * result + Objects.hashCode(this.transformation);
    }

    public record Entry(float threshold, ItemModel model) {
    }

    private static class Factory implements ItemModelFactory<RangeDispatchItemModel> {

        @Override
        public RangeDispatchItemModel create(Pack pack, Path path, ConfigSection section, Consumer<ModelGenerationHolder> generations) {
            RangeDispatchProperty property = RangeDispatchProperties.fromConfig(section);
            float scale = section.getFloat("scale", 1.0f);
            ItemModel fallbackModel = section.getValue("fallback", v -> ItemModels.fromConfig(pack, path, v, generations));
            List<Entry> entryList = new ArrayList<>();
            ConfigValue entries = section.getNonNullValue("entries", ConfigConstants.ARGUMENT_LIST);
            entries.forEach(value -> {
                ConfigSection entry = value.getAsSection();
                float threshold = entry.getNonNullFloat("threshold");
                ItemModel model = entry.getValue("model", v -> ItemModels.fromConfig(pack, path, v, generations), fallbackModel);
                entryList.add(new Entry(threshold, model));
            });
            entryList.sort(Comparator.comparingDouble(Entry::threshold));
            return new RangeDispatchItemModel(
                    property,
                    scale,
                    entryList,
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
            List<Entry> entries = new ArrayList<>(entriesArray.size());
            for (JsonElement entry : entriesArray) {
                if (entry instanceof JsonObject entryObj) {
                    float threshold = entryObj.getAsJsonPrimitive("threshold").getAsFloat();
                    ItemModel model = ItemModels.fromJson(entryObj.getAsJsonObject("model"));
                    entries.add(new Entry(threshold, model));
                } else {
                    throw new IllegalArgumentException("range entry must be an object");
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
