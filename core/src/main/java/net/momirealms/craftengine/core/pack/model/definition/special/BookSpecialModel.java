package net.momirealms.craftengine.core.pack.model.definition.special;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.revision.Revision;
import net.momirealms.craftengine.core.pack.revision.Revisions;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.util.function.Consumer;

public final class BookSpecialModel implements SpecialModel {
    public static final SpecialModelFactory<BookSpecialModel> FACTORY = new Factory();
    public static final SpecialModelReader<BookSpecialModel> READER = new Reader();
    private final float openAngle;
    private final float page1;
    private final float page2;

    public BookSpecialModel(float openAngle, float page1, float page2) {
        this.openAngle = openAngle;
        this.page1 = page1;
        this.page2 = page2;
    }

    public float openAngle() {
        return this.openAngle;
    }

    public float page1() {
        return this.page1;
    }

    public float page2() {
        return this.page2;
    }

    @Override
    public void collectRevision(Consumer<Revision> consumer) {
        consumer.accept(Revisions.SINCE_26_1);
    }

    @Override
    public JsonObject toJson(MinecraftVersion min, MinecraftVersion max) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "book");
        json.addProperty("open_angle", this.openAngle);
        json.addProperty("page1", this.page1);
        json.addProperty("page2", this.page2);
        return json;
    }

    private static class Factory implements SpecialModelFactory<BookSpecialModel> {
        private static final String[] OPEN_ANGLE = {"open_angle", "open-angle"};

        @Override
        public BookSpecialModel create(ConfigSection section) {
            return new BookSpecialModel(
                    MiscUtils.clamp(section.getFloat(OPEN_ANGLE), 0f, 90f),
                    MiscUtils.clamp(section.getFloat("page1"), 0f, 1f),
                    MiscUtils.clamp(section.getFloat("page2"), 0f, 1f)
            );
        }
    }

    private static class Reader implements SpecialModelReader<BookSpecialModel> {
        @Override
        public BookSpecialModel read(JsonObject json) {
            return new BookSpecialModel(
                    json.get("open_angle").getAsFloat(),
                    json.get("page1").getAsFloat(),
                    json.get("page2").getAsFloat()
            );
        }
    }
}
