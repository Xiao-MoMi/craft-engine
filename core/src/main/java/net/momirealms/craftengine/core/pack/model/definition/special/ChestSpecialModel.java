package net.momirealms.craftengine.core.pack.model.definition.special;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.revision.Revision;
import net.momirealms.craftengine.core.pack.revision.Revisions;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.util.function.Consumer;

public final class ChestSpecialModel implements SpecialModel {
    public static final SpecialModelFactory<ChestSpecialModel> FACTORY = new Factory();
    public static final SpecialModelReader<ChestSpecialModel> READER = new Reader();
    private final String texture;
    private final float openness;
    private final String chestType;

    public ChestSpecialModel(String texture, float openness, String chestType) {
        this.texture = texture;
        this.openness = openness;
        this.chestType = chestType;
    }

    public String texture() {
        return this.texture;
    }

    public float openness() {
        return this.openness;
    }

    public String chestType() {
        return this.chestType;
    }

    @Override
    public void collectRevision(Consumer<Revision> consumer) {
        if (this.chestType != null) {
            consumer.accept(Revisions.SINCE_26_1);
        }
    }

    @Override
    public JsonObject toJson(MinecraftVersion min, MinecraftVersion max) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "chest");
        json.addProperty("texture", this.texture);
        if (this.openness > 0) {
            json.addProperty("openness", this.openness);
        }
        if (min.isAtOrAbove(MinecraftVersion.V26_1) && this.chestType != null) {
            json.addProperty("chest_type", this.chestType);
        }
        return json;
    }

    private static class Factory implements SpecialModelFactory<ChestSpecialModel> {
        private static final String[] CHEST_TYPE = new String[]{"chest_type", "chest-type"};

        @Override
        public ChestSpecialModel create(ConfigSection section) {
            return new ChestSpecialModel(
                    section.getNonNullIdentifier("texture").asMinimalString(),
                    MiscUtils.clamp(section.getFloat("openness"), 0f, 1f),
                    section.getString(CHEST_TYPE)
            );
        }
    }

    private static class Reader implements SpecialModelReader<ChestSpecialModel> {
        @Override
        public ChestSpecialModel read(JsonObject json) {
            return new ChestSpecialModel(
                    json.get("texture").getAsString(),
                    json.has("openness") ? json.get("openness").getAsFloat() : 0,
                    json.has("chest_type") ? json.get("chest_type").getAsString() : null
            );
        }
    }
}
