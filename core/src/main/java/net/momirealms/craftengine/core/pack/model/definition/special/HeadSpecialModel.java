package net.momirealms.craftengine.core.pack.model.definition.special;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.revision.Revision;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class HeadSpecialModel implements SpecialModel {
    public static final SpecialModelFactory<HeadSpecialModel> FACTORY = new Factory();
    public static final SpecialModelReader<HeadSpecialModel> READER = new Reader();
    private final String kind;
    private final String texture;
    private final float animation;

    public HeadSpecialModel(String kind, String texture, float animation) {
        this.kind = kind;
        this.texture = texture;
        this.animation = animation;
    }

    public String kind() {
        return this.kind;
    }

    public String texture() {
        return this.texture;
    }

    public float animation() {
        return this.animation;
    }

    @Override
    public List<Revision> revisions() {
        return List.of();
    }

    @Override
    public JsonObject apply(MinecraftVersion version) {
        if (version.isAtOrAbove(MinecraftVersion.V1_21_6) && this.kind.equals("player")) {
            JsonObject json = new JsonObject();
            json.addProperty("type", "player_head");
            return json;
        } else {
            JsonObject json = new JsonObject();
            json.addProperty("type", "head");
            json.addProperty("kind", this.kind);
            if (this.texture != null) {
                json.addProperty("texture", this.texture);
            }
            if (this.animation != 0) {
                json.addProperty("animation", this.animation);
            }
            return json;
        }
    }

    private static class Factory implements SpecialModelFactory<HeadSpecialModel> {
        @Override
        public HeadSpecialModel create(Map<String, Object> arguments) {
            String kind = ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("kind"), "warning.config.item.model.special.head.missing_kind");
            String texture = Optional.ofNullable(arguments.get("texture")).map(String::valueOf).orElse(null);
            float animation = ResourceConfigUtils.getAsFloat(arguments.getOrDefault("animation", 0), "animation");
            return new HeadSpecialModel(kind, texture, animation);
        }
    }

    private static class Reader implements SpecialModelReader<HeadSpecialModel> {
        @Override
        public HeadSpecialModel read(JsonObject json) {
            String kind = json.get("kind").getAsString();
            String texture = json.has("texture") ? json.get("texture").getAsString() : null;
            float animation = json.has("animation") ? json.get("animation").getAsFloat() : 0f;
            return new HeadSpecialModel(kind, texture, animation);
        }
    }
}
