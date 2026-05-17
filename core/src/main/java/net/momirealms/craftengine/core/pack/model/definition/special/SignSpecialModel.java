package net.momirealms.craftengine.core.pack.model.definition.special;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.revision.Revision;
import net.momirealms.craftengine.core.pack.revision.Revisions;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MinecraftVersion;

import java.util.function.Consumer;

public final class SignSpecialModel implements SpecialModel {
    public static final SpecialModelFactory<SignSpecialModel> FACTORY = new Factory();
    public static final SpecialModelReader<SignSpecialModel> READER = new Reader();
    private final Key type;
    private final String attachment;
    private final String woodType;
    private final String texture;

    public SignSpecialModel(Key type, String attachment, String woodType, String texture) {
        this.type = type;
        this.attachment = attachment;
        this.woodType = woodType;
        this.texture = texture;
    }

    public Key type() {
        return this.type;
    }

    public String attachment() {
        return this.attachment;
    }

    public String woodType() {
        return this.woodType;
    }

    public String texture() {
        return this.texture;
    }

    @Override
    public void collectRevision(Consumer<Revision> consumer) {
        if (attachment != null) {
            consumer.accept(Revisions.SINCE_26_1);
        }
    }

    @Override
    public JsonObject toJson(MinecraftVersion min, MinecraftVersion max) {
        JsonObject json = new JsonObject();
        json.addProperty("type", this.type.asMinimalString());
        if (min.isAtOrAbove(MinecraftVersion.V26_1) && attachment != null) {
            json.addProperty("attachment", attachment);
        }
        json.addProperty("wood_type", woodType);
        if (texture != null) {
            json.addProperty("texture", texture);
        }
        return json;
    }

    private static class Factory implements SpecialModelFactory<SignSpecialModel> {
        private static final String[] WOOD_TYPES = new String[] {"wood_type", "wood-type"};

        @Override
        public SignSpecialModel create(ConfigSection section) {
            return new SignSpecialModel(
                    section.getNonNullIdentifier("type"),
                    section.getString("attachment"),
                    section.getNonNullString(WOOD_TYPES),
                    section.getString("texture")
            );
        }
    }

    private static class Reader implements SpecialModelReader<SignSpecialModel> {
        @Override
        public SignSpecialModel read(JsonObject json) {
            Key type = Key.of(json.get("type").toString());
            String attachment = json.has("attachment") ? json.get("attachment").getAsString() : null;
            String woodType = json.get("wood_type").getAsString();
            String texture = json.has("texture") ? json.get("texture").getAsString() : null;
            return new SignSpecialModel(type, attachment, woodType, texture);
        }
    }
}
