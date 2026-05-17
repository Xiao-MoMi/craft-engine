package net.momirealms.craftengine.core.pack.model.definition.special;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.revision.Revision;
import net.momirealms.craftengine.core.pack.revision.Revisions;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.MinecraftVersion;

import java.util.function.Consumer;

public final class PlayerHeadSpecialModel implements SpecialModel {
    public static final SpecialModelFactory<PlayerHeadSpecialModel> FACTORY = new Factory();
    public static final SpecialModelReader<PlayerHeadSpecialModel> READER = new Reader();
    public static final PlayerHeadSpecialModel INSTANCE = new PlayerHeadSpecialModel();

    private PlayerHeadSpecialModel() {}

    @Override
    public void collectRevision(Consumer<Revision> consumer) {
        consumer.accept(Revisions.SINCE_1_21_6);
    }

    @Override
    public JsonObject toJson(MinecraftVersion min, MinecraftVersion max) {
        JsonObject json = new JsonObject();
        if (min.isAtOrAbove(MinecraftVersion.V1_21_6)) {
            json.addProperty("type", "player_head");
        } else {
            json.addProperty("type", "head");
            json.addProperty("kind", "player");
        }
        return json;
    }

    private static class Factory implements SpecialModelFactory<PlayerHeadSpecialModel> {
        @Override
        public PlayerHeadSpecialModel create(ConfigSection section) {
            return INSTANCE;
        }
    }

    private static class Reader implements SpecialModelReader<PlayerHeadSpecialModel> {
        @Override
        public PlayerHeadSpecialModel read(JsonObject json) {
            return INSTANCE;
        }
    }
}
