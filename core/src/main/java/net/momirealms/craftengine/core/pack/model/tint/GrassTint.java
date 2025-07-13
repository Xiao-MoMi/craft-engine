package net.momirealms.craftengine.core.pack.model.tint;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.util.Map;

public class GrassTint implements Tint {
    public static final Factory FACTORY = new Factory();
    public static final Reader READER = new Reader();
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
            float temperature = ResourceConfigUtils.getAsFloat(arguments.getOrDefault("temperature", 0), "temperature");
            float downfall = ResourceConfigUtils.getAsFloat(arguments.getOrDefault("downfall", 0), "downfall");
            if (temperature > 1 || temperature < 0) {
                throw new LocalizedResourceConfigException("warning.config.item.model.tint.grass.invalid_temp", String.valueOf(temperature));
            }
            if (downfall > 1 || downfall < 0) {
                throw new LocalizedResourceConfigException("warning.config.item.model.tint.grass.invalid_downfall", String.valueOf(downfall));
            }
            return new GrassTint(temperature, downfall);
        }
    }

    public static class Reader implements TintReader {
        @Override
        public Tint read(JsonObject json) {
            float temperature = json.has("temperature") ? json.get("temperature").getAsFloat() : 0;
            float downfall = json.has("downfall") ? json.get("downfall").getAsFloat() : 0;
            return new GrassTint(temperature, downfall);
        }
    }
}
