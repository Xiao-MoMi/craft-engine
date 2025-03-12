package net.momirealms.craftengine.core.pack.model.special;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.util.Map;
import java.util.Objects;

public class HeadSpecialModel implements SpecialModel {
    public static final Factory FACTORY = new Factory();
    private final String kind;
    private final String texture;
    private final int animation;

    public HeadSpecialModel(String kind, String texture, int animation) {
        this.kind = kind;
        this.texture = texture;
        this.animation = animation;
    }

    @Override
    public Key type() {
        return SpecialModels.HEAD;
    }

    @Override
    public JsonObject get() {
        JsonObject json = new JsonObject();
        json.addProperty("type", type().toString());
        json.addProperty("kind", kind);
        json.addProperty("texture", texture);
        json.addProperty("animation", animation);
        return json;
    }

    public static class Factory implements SpecialModelFactory {

        @Override
        public SpecialModel create(Map<String, Object> arguments) {
            String kind = Objects.requireNonNull(arguments.get("kind"), "kind").toString();
            String texture = Objects.requireNonNull(arguments.get("texture"), "texture").toString();
            int animation = MiscUtils.getAsInt(arguments.getOrDefault("animation", 0));
            return new HeadSpecialModel(kind, texture, animation);
        }
    }
}
