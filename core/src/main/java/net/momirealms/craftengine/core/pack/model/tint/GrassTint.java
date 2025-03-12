package net.momirealms.craftengine.core.pack.model.tint;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.util.Map;

public class GrassTint implements Tint {
    public static final Factory FACTORY = new Factory();
    private final float temperature;
    private final float downfall;

    public GrassTint(float temperature, float downfall) {
        this.temperature = temperature;
        this.downfall = downfall;
    }

    @Override
    public Key type() {
        return Tints.GRASS;
    }

    @Override
    public JsonObject get() {
        JsonObject json = new JsonObject();
        json.addProperty("type", type().toString());
        json.addProperty("temperature", temperature);
        json.addProperty("downfall", downfall);
        return json;
    }

    public static class Factory implements TintFactory {

        @Override
        public Tint create(Map<String, Object> arguments) {
            float temperature = MiscUtils.getAsFloat(arguments.getOrDefault("temperature", 0));
            float downfall = MiscUtils.getAsFloat(arguments.getOrDefault("downfall", 0));
            if (temperature > 1 || temperature < 0) {
                throw new IllegalArgumentException("Invalid temperature: " + temperature + ". Valid range 0~1");
            }
            if (downfall > 1 || downfall < 0) {
                throw new IllegalArgumentException("Invalid downfall: " + downfall + ". Valid range 0~1");
            }
            return new GrassTint(temperature, downfall);
        }
    }
}
