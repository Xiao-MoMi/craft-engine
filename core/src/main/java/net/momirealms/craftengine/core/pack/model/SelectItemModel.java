package net.momirealms.craftengine.core.pack.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.core.pack.model.generation.ModelGeneration;
import net.momirealms.craftengine.core.pack.model.select.SelectProperties;
import net.momirealms.craftengine.core.pack.model.select.SelectProperty;
import net.momirealms.craftengine.core.pack.revision.Revision;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import net.momirealms.craftengine.core.util.MiscUtils;
import org.incendo.cloud.type.Either;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SelectItemModel implements ItemModel {
    public static final Factory FACTORY = new Factory();
    public static final Reader READER = new Reader();
    private final SelectProperty property;
    private final Map<Either<String, List<String>>, ItemModel> whenMap;
    private final ItemModel fallBack;

    public SelectItemModel(@NotNull SelectProperty property,
                           @NotNull Map<Either<String, List<String>>, ItemModel> whenMap,
                           @Nullable ItemModel fallBack) {
        this.property = property;
        this.whenMap = whenMap;
        this.fallBack = fallBack;
    }

    public SelectProperty property() {
        return this.property;
    }

    public Map<Either<String, List<String>>, ItemModel> whenMap() {
        return this.whenMap;
    }

    public ItemModel fallBack() {
        return this.fallBack;
    }

    @Override
    public JsonObject apply(MinecraftVersion version) {
        JsonObject json = new JsonObject();
        json.addProperty("type", type().toString());
        this.property.accept(json);
        JsonArray array = new JsonArray();
        json.add("cases", array);
        for (Map.Entry<Either<String, List<String>>, ItemModel> entry : this.whenMap.entrySet()) {
            JsonObject item = new JsonObject();
            ItemModel itemModel = entry.getValue();
            item.add("model", itemModel.apply(version));
            Either<String, List<String>> either = entry.getKey();
            if (either.primary().isPresent()) {
                item.addProperty("when", either.primary().get());
            } else {
                List<String> list = either.fallback().get();
                JsonArray whens = new JsonArray();
                for (String e : list) {
                    whens.add(e);
                }
                item.add("when", whens);
            }
            array.add(item);
        }
        if (this.fallBack != null) {
            json.add("fallback", this.fallBack.apply(version));
        }
        return json;
    }

    @Override
    public Key type() {
        return ItemModels.SELECT;
    }

    @Override
    public List<Revision> revisions() {
        List<Revision> versions = new ArrayList<>();
        if (this.fallBack != null) {
            versions.addAll(this.fallBack.revisions());
        }
        for (ItemModel itemModel : this.whenMap.values()) {
            versions.addAll(itemModel.revisions());
        }
        return versions;
    }

    @Override
    public List<ModelGeneration> modelsToGenerate() {
        List<ModelGeneration> models = new ArrayList<>(4);
        if (this.fallBack != null) {
            models.addAll(this.fallBack.modelsToGenerate());
        }
        for (ItemModel itemModel : this.whenMap.values()) {
            models.addAll(itemModel.modelsToGenerate());
        }
        return models;
    }

    public static class Factory implements ItemModelFactory {

        @SuppressWarnings("unchecked")
        @Override
        public ItemModel create(Map<String, Object> arguments) {
            SelectProperty property = SelectProperties.fromMap(arguments);
            Map<String, Object> fallback = MiscUtils.castToMap(arguments.get("fallback"), true);
            Object casesObj = arguments.get("cases");
            if (casesObj instanceof List<?> list) {
                List<Map<String, Object>> cases = (List<Map<String, Object>>) list;
                if (!cases.isEmpty()) {
                    Map<Either<String, List<String>>, ItemModel> whenMap = new HashMap<>();
                    for (Map<String, Object> c : cases) {
                        Object when = c.get("when");
                        if (when == null) {
                            throw new LocalizedResourceConfigException("warning.config.item.model.select.case.missing_when");
                        }
                        Either<String, List<String>> either;
                        if (when instanceof List<?> whenList) {
                            List<String> whens = new ArrayList<>(whenList.size());
                            for (Object o : whenList) {
                                whens.add(o.toString());
                            }
                            either = Either.ofFallback(whens);
                        } else {
                            either = Either.ofPrimary(when.toString());
                        }
                        Object model = c.get("model");
                        if (model == null) {
                            throw new LocalizedResourceConfigException("warning.config.item.model.select.case.missing_model");
                        }
                        whenMap.put(either, ItemModels.fromMap(MiscUtils.castToMap(model, false)));
                    }
                    return new SelectItemModel(property, whenMap, fallback == null ? null : ItemModels.fromMap(fallback));
                } else {
                    throw new LocalizedResourceConfigException("warning.config.item.model.select.missing_cases");
                }
            } else {
                throw new LocalizedResourceConfigException("warning.config.item.model.select.missing_cases");
            }
        }
    }

    public static class Reader implements ItemModelReader {

        @Override
        public ItemModel read(JsonObject json) {
            JsonArray cases = json.getAsJsonArray("cases");
            if (cases == null) {
                throw new IllegalArgumentException("cases is expected to be a JsonArray");
            }
            Map<Either<String, List<String>>, ItemModel> whenMap = new HashMap<>(cases.size());
            for (JsonElement e : cases) {
                if (e instanceof JsonObject caseObj) {
                    ItemModel model = ItemModels.fromJson(caseObj.getAsJsonObject("model"));
                    JsonElement whenObj = caseObj.get("when");
                    Either<String, List<String>> either;
                    if (whenObj instanceof JsonArray array) {
                        List<String> whens = new ArrayList<>(array.size());
                        for (JsonElement o : array) {
                            whens.add(o.getAsString());
                        }
                        either = Either.ofFallback(whens);
                    } else if (whenObj instanceof JsonPrimitive primitive) {
                        either = Either.ofPrimary(primitive.getAsString());
                    } else {
                        throw new IllegalArgumentException("when is expected to be either JsonPrimitive or JsonArray");
                    }
                    whenMap.put(either, model);
                } else {
                    throw new IllegalArgumentException("case is expected to be a JsonObject");
                }
            }
            return new SelectItemModel(SelectProperties.fromJson(json), whenMap, json.has("fallback") ? ItemModels.fromJson(json.getAsJsonObject("fallback")) : null);
        }
    }
}
