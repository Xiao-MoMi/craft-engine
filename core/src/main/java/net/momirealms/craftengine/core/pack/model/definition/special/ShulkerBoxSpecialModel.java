package net.momirealms.craftengine.core.pack.model.definition.special;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.revision.Revision;
import net.momirealms.craftengine.core.pack.revision.Revisions;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import net.momirealms.craftengine.core.util.MiscUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.function.Consumer;

public final class ShulkerBoxSpecialModel implements SpecialModel {
    public static final SpecialModelFactory<ShulkerBoxSpecialModel> FACTORY = new Factory();
    public static final SpecialModelReader<ShulkerBoxSpecialModel> READER = new Reader();
    private final String texture;
    private final float openness;
    private final Direction orientation;

    public ShulkerBoxSpecialModel(String texture, float openness, @Nullable Direction orientation) {
        this.texture = texture;
        this.openness = openness;
        this.orientation = orientation;
    }

    public String texture() {
        return this.texture;
    }

    public float openness() {
        return this.openness;
    }

    public Direction orientation() {
        return this.orientation;
    }

    @Override
    public void collectRevision(Consumer<Revision> consumer) {
        if (this.orientation != null) {
            consumer.accept(Revisions.From_1_21_4_To_1_21_11);
        }
    }

    @Override
    public JsonObject toJson(MinecraftVersion min, MinecraftVersion max) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "shulker_box");
        json.addProperty("texture", this.texture);
        if (min.isBelow(MinecraftVersion.V26_1) && this.orientation != null) {
            json.addProperty("orientation", this.orientation.name().toLowerCase(Locale.ROOT));
        }
        json.addProperty("openness", this.openness);
        return json;
    }

    private static class Factory implements SpecialModelFactory<ShulkerBoxSpecialModel> {
        @Override
        public ShulkerBoxSpecialModel create(ConfigSection section) {
            return new ShulkerBoxSpecialModel(
                    section.getNonNullIdentifier("texture").asMinimalString(),
                    MiscUtils.clamp(section.getFloat("openness"), 0f, 1f),
                    section.getEnum("orientation", Direction.class)
            );
        }
    }

    private static class Reader implements SpecialModelReader<ShulkerBoxSpecialModel> {
        @Override
        public ShulkerBoxSpecialModel read(JsonObject json) {
            return new ShulkerBoxSpecialModel(
                    json.get("texture").getAsString(),
                    json.has("openness") ? json.get("openness").getAsFloat() : 0f,
                    json.has("orientation") ? Direction.valueOf(json.get("orientation").getAsString().toUpperCase(Locale.ROOT)) : null
            );
        }
    }
}
