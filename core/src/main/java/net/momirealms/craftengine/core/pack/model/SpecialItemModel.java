package net.momirealms.craftengine.core.pack.model;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.model.generator.ModelGeneration;
import net.momirealms.craftengine.core.pack.model.special.SpecialModel;
import net.momirealms.craftengine.core.pack.model.special.SpecialModels;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SpecialItemModel implements ItemModel {
    public static final Factory FACTORY = new Factory();
    private final SpecialModel specialModel;
    private final String base;

    public SpecialItemModel(SpecialModel specialModel, String base) {
        this.specialModel = specialModel;
        this.base = base;
    }

    public SpecialModel specialModel() {
        return specialModel;
    }

    public String base() {
        return base;
    }

    @Override
    public JsonObject get() {
        JsonObject json = new JsonObject();
        json.addProperty("type", type().toString());
        json.add("model", specialModel.get());
        json.addProperty("base", base);
        return json;
    }

    @Override
    public Key type() {
        return ItemModels.SPECIAL;
    }

    @Override
    public List<ModelGeneration> modelsToGenerate() {
        return List.of();
    }

    public static class Factory implements ItemModelFactory {

        @Override
        public ItemModel create(Map<String, Object> arguments) {
            String base = Objects.requireNonNull(arguments.get("base")).toString();
            Map<String, Object> model = MiscUtils.castToMap(arguments.get("model"), false);
            return new SpecialItemModel(SpecialModels.fromMap(model), base);
        }
    }
}
