package net.momirealms.craftengine.core.pack.model.special;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.revision.Revision;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.util.List;
import java.util.Map;

public class ChestSpecialModel implements SpecialModel {
    public static final Factory FACTORY = new Factory();
    public static final Reader READER = new Reader();
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
    public List<Revision> revisions() {
        return List.of();
    }

    @Override
    public JsonObject apply(MinecraftVersion version) {
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

    public static class Reader implements SpecialModelReader {
        @Override
        public SpecialModel read(JsonObject json) {
            float openness = json.has("openness") ? json.get("openness").getAsFloat() : 0;
            return new ChestSpecialModel(json.get("texture").getAsString(), openness);
        }
    }
}
