package net.momirealms.craftengine.core.pack.model.special;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.revision.Revision;
import net.momirealms.craftengine.core.pack.revision.Revisions;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import net.momirealms.craftengine.core.util.MinecraftVersions;

import java.util.List;
import java.util.Map;

public class PlayerHeadSpecialModel implements SpecialModel {
    public static final Factory FACTORY = new Factory();
    public static final Reader READER = new Reader();
    public static final PlayerHeadSpecialModel INSTANCE = new PlayerHeadSpecialModel();

    public PlayerHeadSpecialModel() {
    }

    @Override
    public Key type() {
        return SpecialModels.PLAYER_HEAD;
    }

    @Override
    public List<Revision> revisions() {
        return List.of(Revisions.SINCE_1_21_6);
    }

    @Override
    public JsonObject apply(MinecraftVersion version) {
        JsonObject json = new JsonObject();
        if (version.isAtOrAbove(MinecraftVersions.V1_21_6)) {
            json.addProperty("type", type().toString());
        } else {
            json.addProperty("type", SpecialModels.HEAD.toString());
            json.addProperty("kind", "player");
        }
        return json;
    }

    public static class Factory implements SpecialModelFactory {
        @Override
        public SpecialModel create(Map<String, Object> arguments) {
            return INSTANCE;
        }
    }

    public static class Reader implements SpecialModelReader {
        @Override
        public SpecialModel read(JsonObject json) {
            return INSTANCE;
        }
    }
}
