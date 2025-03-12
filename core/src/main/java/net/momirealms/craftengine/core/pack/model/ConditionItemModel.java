package net.momirealms.craftengine.core.pack.model;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.model.condition.ConditionProperties;
import net.momirealms.craftengine.core.pack.model.condition.ConditionProperty;
import net.momirealms.craftengine.core.pack.model.generator.ModelGeneration;
import net.momirealms.craftengine.core.util.Key;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ConditionItemModel implements ItemModel {
    public static final Factory FACTORY = new Factory();
    private final ConditionProperty property;
    private final ItemModel onTrue;
    private final ItemModel onFalse;

    public ConditionItemModel(ConditionProperty property, ItemModel onTrue, ItemModel onFalse) {
        this.property = property;
        this.onTrue = onTrue;
        this.onFalse = onFalse;
    }

    public ConditionProperty property() {
        return property;
    }

    public ItemModel onTrue() {
        return onTrue;
    }

    public ItemModel onFalse() {
        return onFalse;
    }

    @Override
    public Key type() {
        return ItemModels.CONDITION;
    }

    @Override
    public List<ModelGeneration> modelsToGenerate() {
        List<ModelGeneration> models = new ArrayList<>(4);
        models.addAll(onTrue.modelsToGenerate());
        models.addAll(onFalse.modelsToGenerate());
        return models;
    }

    @Override
    public JsonObject get() {
        JsonObject json = new JsonObject();
        json.addProperty("type", type().toString());
        json.add("on_true", onTrue.get());
        json.add("on_false", onFalse.get());
        property.accept(json);
        return json;
    }

    public static class Factory implements ItemModelFactory {

        @SuppressWarnings("unchecked")
        @Override
        public ItemModel create(Map<String, Object> arguments) {
            ConditionProperty property = ConditionProperties.fromMap(arguments);
            Map<String, Object> onTrue = Objects.requireNonNull((Map<String, Object>) arguments.get("on-true"));
            Map<String, Object> onFalse = Objects.requireNonNull((Map<String, Object>) arguments.get("on-false"));
            return new ConditionItemModel(property, ItemModels.fromMap(onTrue), ItemModels.fromMap(onFalse));
        }
    }
}
