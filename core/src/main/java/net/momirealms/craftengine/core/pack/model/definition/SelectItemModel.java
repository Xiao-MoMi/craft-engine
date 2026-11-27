package net.momirealms.craftengine.core.pack.model.definition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.pack.model.definition.select.SelectProperties;
import net.momirealms.craftengine.core.pack.model.definition.select.SelectProperty;
import net.momirealms.craftengine.core.pack.model.generation.ModelGenerationHolder;
import net.momirealms.craftengine.core.pack.revision.Revision;
import net.momirealms.craftengine.core.pack.revision.Revisions;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

public final class SelectItemModel implements ItemModel {
    public static final ItemModelFactory<SelectItemModel> FACTORY = new Factory();
    public static final ItemModelReader<SelectItemModel> READER = new Reader();
    private final SelectProperty property;
    private final Map<Either<JsonElement, List<JsonElement>>, ItemModel> whenMap;
    private final ItemModel fallBack;
    private final Transformation transformation;

    public SelectItemModel(@NotNull SelectProperty property,
                           @NotNull Map<Either<JsonElement, List<JsonElement>>, ItemModel> whenMap,
                           @Nullable ItemModel fallBack,
                           @Nullable Transformation transformation) {
        this.property = property;
        this.whenMap = whenMap;
        this.fallBack = fallBack;
        this.transformation = transformation;
    }

    public SelectItemModel(@NotNull SelectProperty property,
                           @NotNull Map<Either<JsonElement, List<JsonElement>>, ItemModel> whenMap,
                           @Nullable ItemModel fallBack) {
        this(property, whenMap, fallBack, null);
    }

    @Nullable
    public Transformation transformation() {
        return this.transformation;
    }

    @NotNull
    public SelectProperty property() {
        return this.property;
    }

    @NotNull
    public Map<Either<JsonElement, List<JsonElement>>, ItemModel> whenMap() {
        return this.whenMap;
    }

    @Nullable
    public ItemModel fallBack() {
        return this.fallBack;
    }

    @Override
    public JsonObject toJson(MinecraftVersion min, MinecraftVersion max) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "select");
        this.property.writeProperty(json);
        JsonArray array = new JsonArray();
        json.add("cases", array);
        for (Map.Entry<Either<JsonElement, List<JsonElement>>, ItemModel> entry : this.whenMap.entrySet()) {
            JsonObject item = new JsonObject();
            Either<JsonElement, List<JsonElement>> either = entry.getKey();
            either.ifLeft(left -> {
                JsonElement remap = this.property.remap(left, min);
                if (remap != null) {
                    item.add("when", remap);
                }
            }).ifRight(right -> {
                JsonArray whens = new JsonArray();
                for (JsonElement e : right) {
                    JsonElement remap = this.property.remap(e, min);
                    if (remap != null) {
                        whens.add(remap);
                    }
                }
                if (!whens.isEmpty()) {
                    item.add("when", whens);
                }
            });
            if (item.has("when")) {
                ItemModel itemModel = entry.getValue();
                item.add("model", itemModel.toJson(min, max));
                array.add(item);
            }
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
        for (Map.Entry<Either<JsonElement, List<JsonElement>>, ItemModel> entry : this.whenMap.entrySet()) {
            Either<JsonElement, List<JsonElement>> when = entry.getKey();
            when.ifLeft(left -> {
                this.property.gatherRevisions(left, consumer);
            }).ifRight(right -> {
                for (JsonElement e : right) {
                    this.property.gatherRevisions(e, consumer);
                }
            });
            entry.getValue().gatherRevisions(consumer);
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof SelectItemModel that)) {
            return false;
        }
        if (!Objects.equals(this.property, that.property)
                || !Objects.equals(this.fallBack, that.fallBack)
                || !Objects.equals(this.transformation, that.transformation)
                || this.whenMap.size() != that.whenMap.size()) {
            return false;
        }
        Iterator<Map.Entry<Either<JsonElement, List<JsonElement>>, ItemModel>> first = this.whenMap.entrySet().iterator();
        Iterator<Map.Entry<Either<JsonElement, List<JsonElement>>, ItemModel>> second = that.whenMap.entrySet().iterator();
        while (first.hasNext()) {
            if (!first.next().equals(second.next())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(this.property);
        for (Map.Entry<Either<JsonElement, List<JsonElement>>, ItemModel> entry : this.whenMap.entrySet()) {
            result = 31 * result + entry.hashCode();
        }
        result = 31 * result + Objects.hashCode(this.fallBack);
        return 31 * result + Objects.hashCode(this.transformation);
    }

    private static class Factory implements ItemModelFactory<SelectItemModel> {

        @Override
        public SelectItemModel create(Pack pack, Path path, ConfigSection section, Consumer<ModelGenerationHolder> generations) {
            SelectProperty property = SelectProperties.fromConfig(section);
            ItemModel fallbackModel = section.getValue("fallback", v -> ItemModels.fromConfig(pack, path, v, generations));
            Map<Either<JsonElement, List<JsonElement>>, ItemModel> whenMap = new LinkedHashMap<>();
            ConfigValue cases = section.getNonNullValue("cases", ConfigConstants.ARGUMENT_LIST);
            cases.forEach(value -> {
                ConfigSection entry = value.getAsSection();
                List<JsonElement> when = entry.getNonEmptyList("when", v -> GsonHelper.get().toJsonTree(v.value()));
                Either<JsonElement, List<JsonElement>> either;
                if (when.size() == 1) {
                    either = Either.left(when.getFirst());
                } else {
                    either = Either.right(when);
                }
                ItemModel model = entry.getNonNullValue("model", ConfigConstants.ARGUMENT_ITEM_MODEL_DEFINITION, v -> ItemModels.fromConfig(pack, path, v, generations));
                whenMap.put(either, model);
            });
            return new SelectItemModel(
                    property,
                    whenMap,
                    fallbackModel,
                    section.getValue("transformation", Transformation::fromConfig)
            );
        }
    }

    private static class Reader implements ItemModelReader<SelectItemModel> {

        @Override
        public SelectItemModel read(JsonObject json) {
            JsonArray cases = json.getAsJsonArray("cases");
            if (cases == null || cases.isEmpty()) {
                throw new IllegalArgumentException("cases must be a non-empty JsonArray");
            }
            Map<Either<JsonElement, List<JsonElement>>, ItemModel> whenMap = new LinkedHashMap<>(cases.size());
            Set<JsonElement> values = new HashSet<>();
            for (JsonElement caseElement : cases) {
                if (!(caseElement instanceof JsonObject caseObj)) {
                    throw new IllegalArgumentException("case is expected to be a JsonObject");
                }
                JsonObject modelJson = caseObj.getAsJsonObject("model");
                if (modelJson == null) {
                    throw new IllegalArgumentException("model is expected to be a JsonObject");
                }
                ItemModel model = ItemModels.fromJson(modelJson);
                JsonElement whenObj = caseObj.get("when");
                Either<JsonElement, List<JsonElement>> either;
                if (whenObj instanceof JsonArray array) {
                    if (array.isEmpty()) {
                        throw new IllegalArgumentException("'when' must not be empty");
                    }
                    List<JsonElement> whens = new ArrayList<>(array.size());
                    for (JsonElement o : array) {
                        whens.add(o);
                    }
                    either = Either.right(whens);
                } else if (whenObj != null) {
                    either = Either.left(whenObj);
                } else {
                    throw new IllegalArgumentException("'when' should not be null");
                }
                for (JsonElement value : either.map(List::of, list -> list)) {
                    if (!values.add(value)) {
                        throw new IllegalArgumentException("Duplicate select case value: " + value);
                    }
                }
                whenMap.put(either, model);
            }
            return new SelectItemModel(
                    SelectProperties.fromJson(json),
                    whenMap,
                    json.has("fallback") ? ItemModels.fromJson(json.getAsJsonObject("fallback")) : null,
                    json.has("transformation") ? Transformation.fromJson(json.get("transformation")) : null
            );
        }
    }
}
