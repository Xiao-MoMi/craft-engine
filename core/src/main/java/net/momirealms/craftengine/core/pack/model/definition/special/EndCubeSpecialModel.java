package net.momirealms.craftengine.core.pack.model.definition.special;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.revision.Revision;
import net.momirealms.craftengine.core.pack.revision.Revisions;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.Objects;

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

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof EndCubeSpecialModel that)) {
            return false;
        }
        return Objects.equals(this.effect, that.effect);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.effect);
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
