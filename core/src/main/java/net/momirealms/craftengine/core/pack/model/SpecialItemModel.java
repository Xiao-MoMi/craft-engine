package net.momirealms.craftengine.core.pack.model;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.ResourceLocation;
import net.momirealms.craftengine.core.pack.model.generation.ModelGeneration;
import net.momirealms.craftengine.core.pack.model.special.SpecialModel;
import net.momirealms.craftengine.core.pack.model.special.SpecialModels;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class SpecialItemModel implements ItemModel {
    public static final Factory FACTORY = new Factory();
    private final SpecialModel specialModel;
    private final String base;
    private final ModelGeneration modelGeneration;

    public SpecialItemModel(SpecialModel specialModel, String base, @Nullable ModelGeneration generation) {
        this.specialModel = specialModel;
        this.base = base;
        this.modelGeneration = generation;
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
        if (this.modelGeneration == null) {
            return List.of();
        } else {
            return List.of(this.modelGeneration);
        }
    }

    public static class Factory implements ItemModelFactory {

        @Override
        public ItemModel create(Map<String, Object> arguments) {
            String base = ResourceConfigUtils.requireNonEmptyStringOrThrow(ResourceConfigUtils.get(arguments, "base", "path"), "warning.config.item.model.special.missing_path");
            if (!ResourceLocation.isValid(base)) {
                throw new LocalizedResourceConfigException("warning.config.item.model.special.invalid_path", base);
            }
            Map<String, Object> generation = MiscUtils.castToMap(arguments.get("generation"), true);
            ModelGeneration modelGeneration = null;
            if (generation != null) {
                modelGeneration = ModelGeneration.of(Key.of(base), generation);
            }
            Map<String, Object> model = MiscUtils.castToMap(arguments.get("model"), false);
            return new SpecialItemModel(SpecialModels.fromMap(model), base, modelGeneration);
        }
    }
}
