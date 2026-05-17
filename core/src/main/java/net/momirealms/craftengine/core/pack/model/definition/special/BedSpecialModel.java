package net.momirealms.craftengine.core.pack.model.definition.special;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.revision.Revision;
import net.momirealms.craftengine.core.pack.revision.Revisions;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.MinecraftVersion;

import java.util.function.Consumer;

public final class BedSpecialModel implements SpecialModel {
    public static final SpecialModelFactory<BedSpecialModel> FACTORY = new Factory();
    public static final SpecialModelReader<BedSpecialModel> READER = new Reader();
    private final String part;
    private final String texture;

    public BedSpecialModel(String part, String texture) {
        this.part = part;
        this.texture = texture;
    }

    public String part() {
        return this.part;
    }

    public String texture() {
        return this.texture;
    }

    @Override
    public void collectRevision(Consumer<Revision> consumer) {
        if (this.part != null) {
            consumer.accept(Revisions.SINCE_26_1);
        }
    }

    @Override
    public JsonObject toJson(MinecraftVersion min, MinecraftVersion max) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "bed");
        if (min.isAtOrAbove(MinecraftVersion.V26_1)) {
            json.addProperty("part", this.part);
        }
        json.addProperty("texture", this.texture);
        return json;
    }

    private static class Factory implements SpecialModelFactory<BedSpecialModel> {
        @Override
        public BedSpecialModel create(ConfigSection section) {
            return new BedSpecialModel(
                    Config.packMaxVersion().isAtOrAbove(MinecraftVersion.V26_1) ? section.getNonEmptyString("part") : null,
                    section.getNonNullIdentifier("texture").asMinimalString()
            );
        }
    }

    private static class Reader implements SpecialModelReader<BedSpecialModel> {
        @Override
        public BedSpecialModel read(JsonObject json) {
            return new BedSpecialModel(
                    json.has("part") ? json.get("part").getAsString() : null,
                    json.get("texture").getAsString()
            );
        }
    }
}
