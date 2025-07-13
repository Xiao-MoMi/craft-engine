package net.momirealms.craftengine.core.pack.model;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.model.condition.ConditionProperties;
import net.momirealms.craftengine.core.pack.model.condition.ConditionProperty;
import net.momirealms.craftengine.core.pack.model.generation.ModelGeneration;
import net.momirealms.craftengine.core.pack.revision.Revision;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConditionItemModel implements ItemModel {
    public static final Factory FACTORY = new Factory();
    public static final Reader READER = new Reader();
    private final ConditionProperty property;
    private final ItemModel onTrue;
    private final ItemModel onFalse;

    public ConditionItemModel(ConditionProperty property, ItemModel onTrue, ItemModel onFalse) {
        this.property = property;
        this.onTrue = onTrue;
        this.onFalse = onFalse;
    }

    public ConditionProperty property() {
        return this.property;
    }

    public ItemModel onTrue() {
        return this.onTrue;
    }

    public ItemModel onFalse() {
        return this.onFalse;
    }

    @Override
    public Key type() {
        return ItemModels.CONDITION;
    }

    @Override
    public List<Revision> revisions() {
        List<Revision> onTrueVersions = this.onTrue.revisions();
        List<Revision> onFalseVersions = this.onFalse.revisions();
        if (onTrueVersions.isEmpty() && onFalseVersions.isEmpty()) return List.of();
        List<Revision> versions = new ArrayList<>(onTrueVersions.size() + onFalseVersions.size());
        versions.addAll(onTrueVersions);
        versions.addAll(onFalseVersions);
        return versions;
    }

    @Override
    public List<ModelGeneration> modelsToGenerate() {
        List<ModelGeneration> onTrueModels = this.onTrue.modelsToGenerate();
        List<ModelGeneration> onFalseModels = this.onFalse.modelsToGenerate();
        if (onTrueModels.isEmpty() && onFalseModels.isEmpty()) return List.of();
        List<ModelGeneration> models = new ArrayList<>(onTrueModels.size() + onFalseModels.size());
        models.addAll(onTrueModels);
        models.addAll(onFalseModels);
        return models;
    }

    @Override
    public JsonObject apply(MinecraftVersion version) {
        JsonObject json = new JsonObject();
        json.addProperty("type", type().toString());
        json.add("on_true", this.onTrue.apply(version));
        json.add("on_false", this.onFalse.apply(version));
        this.property.accept(json);
        return json;
    }

    public static class Factory implements ItemModelFactory {

        @Override
        public ItemModel create(Map<String, Object> arguments) {
            ConditionProperty property = ConditionProperties.fromMap(arguments);
            ItemModel onTrue;
            if (arguments.get("on-true") instanceof Map<?,?> map1) {
                onTrue = ItemModels.fromMap(MiscUtils.castToMap(map1, false));
            } else {
                throw new LocalizedResourceConfigException("warning.config.item.model.condition.missing_on_true");
            }
            ItemModel onFalse;
            if (arguments.get("on-false") instanceof Map<?,?> map2) {
                onFalse = ItemModels.fromMap(MiscUtils.castToMap(map2, false));
            } else {
                throw new LocalizedResourceConfigException("warning.config.item.model.condition.missing_on_false");
            }
            return new ConditionItemModel(property, onTrue, onFalse);
        }
    }

    public static class Reader implements ItemModelReader {

        @Override
        public ItemModel read(JsonObject json) {
            ConditionProperty property = ConditionProperties.fromJson(json);
            ItemModel onTrue = ItemModels.fromJson(json.getAsJsonObject("on_true"));
            ItemModel onFalse = ItemModels.fromJson(json.getAsJsonObject("on_false"));
            return new ConditionItemModel(property, onTrue, onFalse);
        }
    }
}
