package net.momirealms.craftengine.core.pack.model.special;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.util.Map;

public class ChestSpecialModel implements SpecialModel {
    public static final Factory FACTORY = new Factory();
    private final String texture;
    private final float openness;

    public ChestSpecialModel(String texture, float openness) {
        this.texture = texture;
        this.openness = openness;
    }

    @Override
    public Key type() {
        return SpecialModels.CHEST;
    }

    @Override
    public JsonObject get() {
        JsonObject json = new JsonObject();
        json.addProperty("type", type().toString());
        json.addProperty("texture", texture);
        if (openness > 0) {
            json.addProperty("openness", openness);
        }
        return json;
    }

    public static class Factory implements SpecialModelFactory {

        @Override
        public SpecialModel create(Map<String, Object> arguments) {
            float openness = ResourceConfigUtils.getAsFloat(arguments.getOrDefault("openness", 0), "openness");
            String texture = ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("texture"), "warning.config.item.model.special.chest.missing_texture");
            if (openness > 1 || openness < 0) {
                throw new LocalizedResourceConfigException("warning.config.item.model.special.chest.invalid_openness", String.valueOf(openness));
            }
            return new ChestSpecialModel(texture, openness);
        }
    }
}
