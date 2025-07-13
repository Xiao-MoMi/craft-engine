package net.momirealms.craftengine.core.pack.model.special;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.revision.Revision;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.util.List;
import java.util.Map;

public class BedSpecialModel implements SpecialModel {
    public static final Factory FACTORY = new Factory();
    public static final Reader READER = new Reader();
    private final String texture;

    public BedSpecialModel(String texture) {
        this.texture = texture;
    }

    @Override
    public Key type() {
        return SpecialModels.BED;
    }

    @Override
    public JsonObject apply(MinecraftVersion version) {
        JsonObject json = new JsonObject();
        json.addProperty("type", type().toString());
        json.addProperty("texture", texture);
        return json;
    }

    @Override
    public List<Revision> revisions() {
        return List.of();
    }

    public static class Factory implements SpecialModelFactory {
        @Override
        public SpecialModel create(Map<String, Object> arguments) {
            String texture = ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("texture"), "warning.config.item.model.special.bed.missing_texture");
            return new BedSpecialModel(texture);
        }
    }

    public static class Reader implements SpecialModelReader {
        @Override
        public SpecialModel read(JsonObject json) {
            String texture = json.get("texture").getAsString();
            return new BedSpecialModel(texture);
        }
    }
}
