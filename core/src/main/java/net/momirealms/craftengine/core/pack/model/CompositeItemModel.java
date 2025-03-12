package net.momirealms.craftengine.core.pack.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.model.generator.ModelGeneration;
import net.momirealms.craftengine.core.util.Key;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CompositeItemModel implements ItemModel {
    public static final Factory FACTORY = new Factory();
    private final List<ItemModel> models;

    public CompositeItemModel(List<ItemModel> models) {
        this.models = models;
    }

    public List<ItemModel> models() {
        return models;
    }

    @Override
    public JsonObject get() {
        JsonObject json = new JsonObject();
        json.addProperty("type", type().toString());
        JsonArray array = new JsonArray();
        for (ItemModel model : models) {
            array.add(model.get());
        }
        json.add("models", array);
        return json;
    }

    @Override
    public Key type() {
        return ItemModels.COMPOSITE;
    }

    @Override
    public List<ModelGeneration> modelsToGenerate() {
        List<ModelGeneration> models = new ArrayList<>(4);
        for (ItemModel model : this.models) {
            models.addAll(model.modelsToGenerate());
        }
        return models;
    }

    public static class Factory implements ItemModelFactory {

        @SuppressWarnings("unchecked")
        @Override
        public ItemModel create(Map<String, Object> arguments) {
            List<Map<String, Object>> models = (List<Map<String, Object>>) arguments.get("models");
            List<ItemModel> modelList = new ArrayList<>();
            for (Map<String, Object> model : models) {
                modelList.add(ItemModels.fromMap(model));
            }
            return new CompositeItemModel(modelList);
        }
    }
}
