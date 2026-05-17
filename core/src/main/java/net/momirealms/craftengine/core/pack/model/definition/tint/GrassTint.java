package net.momirealms.craftengine.core.pack.model.definition.tint;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.MiscUtils;

public final class GrassTint implements Tint {
    public static final TintFactory<GrassTint> FACTORY = new Factory();
    public static final TintReader<GrassTint> READER = new Reader();
    private final float temperature;
    private final float downfall;

    public GrassTint(float temperature, float downfall) {
        this.temperature = temperature;
        this.downfall = downfall;
    }

    public float temperature() {
        return this.temperature;
    }

    public float downfall() {
        return this.downfall;
    }

    @Override
    public JsonObject get() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "grass");
        json.addProperty("temperature", this.temperature);
        json.addProperty("downfall", this.downfall);
        return json;
    }

    private static class Factory implements TintFactory<GrassTint> {

        @Override
        public GrassTint create(ConfigSection section) {
            return new GrassTint(
                    MiscUtils.clamp(section.getFloat("temperature"), 0f, 1f),
                    MiscUtils.clamp(section.getFloat("downfall"), 0f, 1f)
            );
        }
    }

    private static class Reader implements TintReader<GrassTint> {
        @Override
        public GrassTint read(JsonObject json) {
            return new GrassTint(
                    json.has("temperature") ? json.get("temperature").getAsFloat() : 0,
                    json.has("downfall") ? json.get("downfall").getAsFloat() : 0
            );
        }
    }
}
