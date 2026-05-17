package net.momirealms.craftengine.core.pack.model.definition.special;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.revision.Revision;
import net.momirealms.craftengine.core.pack.revision.Revisions;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public final class EndCubeSpecialModel implements SpecialModel {
    public static final SpecialModelFactory<EndCubeSpecialModel> FACTORY = new Factory();
    public static final SpecialModelReader<EndCubeSpecialModel> READER = new Reader();
    private final String effect;

    public EndCubeSpecialModel(@NotNull String effect) {
        this.effect = effect;
    }

    @NotNull
    public String effect() {
        return this.effect;
    }

    @Override
    public void collectRevision(Consumer<Revision> consumer) {
        consumer.accept(Revisions.SINCE_26_1);
    }

    @Override
    public JsonObject toJson(MinecraftVersion min, MinecraftVersion max) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "end_cube");
        json.addProperty("effect", this.effect);
        return json;
    }

    private static class Factory implements SpecialModelFactory<EndCubeSpecialModel> {
        @Override
        public EndCubeSpecialModel create(ConfigSection section) {
            return new EndCubeSpecialModel(section.getNonNullString("effect"));
        }
    }

    private static class Reader implements SpecialModelReader<EndCubeSpecialModel> {
        @Override
        public EndCubeSpecialModel read(JsonObject json) {
            return new EndCubeSpecialModel(json.get("effect").getAsString());
        }
    }
}
