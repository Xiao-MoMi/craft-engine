package net.momirealms.craftengine.core.pack.model.definition.special;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.MinecraftVersion;

public final class CopperGolemStatueSpecialModel implements SpecialModel {
    public static final SpecialModelFactory<CopperGolemStatueSpecialModel> FACTORY = new Factory();
    public static final SpecialModelReader<CopperGolemStatueSpecialModel> READER = new Reader();
    private final String pose;
    private final String texture;

    public CopperGolemStatueSpecialModel(String pose, String texture) {
        this.pose = pose;
        this.texture = texture;
    }

    public String pose() {
        return this.pose;
    }

    public String texture() {
        return this.texture;
    }

    @Override
    public JsonObject toJson(MinecraftVersion min, MinecraftVersion max) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "copper_golem_statue");
        json.addProperty("pose", this.pose);
        json.addProperty("texture", this.texture);
        return json;
    }

    private static class Factory implements SpecialModelFactory<CopperGolemStatueSpecialModel> {
        @Override
        public CopperGolemStatueSpecialModel create(ConfigSection section) {
            return new CopperGolemStatueSpecialModel(
                    section.getNonNullString("pose"),
                    section.getNonNullIdentifier("texture").asMinimalString()
            );
        }
    }

    private static class Reader implements SpecialModelReader<CopperGolemStatueSpecialModel> {
        @Override
        public CopperGolemStatueSpecialModel read(JsonObject json) {
            return new CopperGolemStatueSpecialModel(json.get("pose").getAsString(), json.get("texture").getAsString());
        }
    }
}
