package net.momirealms.craftengine.core.pack.model.special;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.util.Map;
import java.util.Objects;

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
        json.addProperty("openness", openness);
        return json;
    }

    public static class Factory implements SpecialModelFactory {

        @Override
        public SpecialModel create(Map<String, Object> arguments) {
            float openness = MiscUtils.getAsFloat(arguments.getOrDefault("openness", 0));
            String texture = Objects.requireNonNull(arguments.get("texture"), "texture").toString();
            if (openness > 1 || openness < 0) {
                throw new IllegalArgumentException("Invalid openness: " + openness + ". Valid range 0~1");
            }
            return new ChestSpecialModel(texture, openness);
        }
    }
}
