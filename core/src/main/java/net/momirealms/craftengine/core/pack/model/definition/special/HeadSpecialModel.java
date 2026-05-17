package net.momirealms.craftengine.core.pack.model.definition.special;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class HeadSpecialModel implements SpecialModel {
    public static final SpecialModelFactory<HeadSpecialModel> FACTORY = new Factory();
    public static final SpecialModelReader<HeadSpecialModel> READER = new Reader();
    private final String kind;
    private final String texture;
    private final float animation;

    public HeadSpecialModel(@NotNull String kind, @Nullable String texture, float animation) {
        this.kind = kind;
        this.texture = texture;
        this.animation = animation;
    }

    @NotNull
    public String kind() {
        return this.kind;
    }

    @Nullable
    public String texture() {
        return this.texture;
    }

    public float animation() {
        return this.animation;
    }

    @Override
    public JsonObject toJson(MinecraftVersion min, MinecraftVersion max) {
        if (min.isAtOrAbove(MinecraftVersion.V1_21_6) && this.kind.equals("player") && this.texture != null) {
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
        public HeadSpecialModel create(ConfigSection section) {
            return new HeadSpecialModel(
                    section.getNonNullString("kind"),
                    section.getValue("texture", v -> v.getAsAssetPath().asMinimalString()),
                    section.getFloat("animation")
            );
        }
    }

    private static class Reader implements SpecialModelReader<HeadSpecialModel> {
        @Override
        public HeadSpecialModel read(JsonObject json) {
            return new HeadSpecialModel(
                    json.get("kind").getAsString(),
                    json.has("texture") ? json.get("texture").getAsString() : null,
                    json.has("animation") ? json.get("animation").getAsFloat() : 0f
            );
        }
    }
}
