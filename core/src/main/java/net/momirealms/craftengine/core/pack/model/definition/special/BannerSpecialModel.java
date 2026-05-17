package net.momirealms.craftengine.core.pack.model.definition.special;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.revision.Revision;
import net.momirealms.craftengine.core.pack.revision.Revisions;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.MinecraftVersion;

import java.util.function.Consumer;

public final class BannerSpecialModel implements SpecialModel {
    public static final SpecialModelFactory<BannerSpecialModel> FACTORY = new Factory();
    public static final SpecialModelReader<BannerSpecialModel> READER = new Reader();
    private final String attachment;
    private final String color;

    public BannerSpecialModel(String attachment, String color) {
        this.attachment = attachment;
        this.color = color;
    }

    public String color() {
        return this.color;
    }

    @Override
    public void collectRevision(Consumer<Revision> consumer) {
        if (this.attachment != null) {
            consumer.accept(Revisions.SINCE_26_1);
        }
    }

    @Override
    public JsonObject toJson(MinecraftVersion min, MinecraftVersion max) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "banner");
        if (min.isAtOrAbove(MinecraftVersion.V26_1) && this.attachment != null) {
            json.addProperty("attachment", this.attachment);
        }
        json.addProperty("color", this.color);
        return json;
    }

    private static class Factory implements SpecialModelFactory<BannerSpecialModel> {
        @Override
        public BannerSpecialModel create(ConfigSection section) {
            return new BannerSpecialModel(
                    section.getString("attachment"),
                    section.getNonNullString("color")
            );
        }
    }

    private static class Reader implements SpecialModelReader<BannerSpecialModel> {
        @Override
        public BannerSpecialModel read(JsonObject json) {
            return new BannerSpecialModel(
                    json.has("attachment") ? json.get("attachment").getAsString() : null,
                    json.get("color").getAsString()
            );
        }
    }
}
