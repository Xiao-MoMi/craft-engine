package net.momirealms.craftengine.core.pack.model.definition.select;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.core.pack.revision.Revision;
import net.momirealms.craftengine.core.pack.revision.Revisions;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public final class DisplayContextSelectProperty implements SelectProperty {
    public static final DisplayContextSelectProperty INSTANCE = new DisplayContextSelectProperty();
    public static final SelectPropertyFactory<DisplayContextSelectProperty> FACTORY = new Factory();
    public static final SelectPropertyReader<DisplayContextSelectProperty> READER = new Reader();

    private DisplayContextSelectProperty() {}

    @Override
    public void writeProperty(JsonObject model) {
        model.addProperty("property", "display_context");
    }

    @Override
    public void gatherRevisions(JsonElement element, Consumer<Revision> consumer) {
        if (element instanceof JsonPrimitive primitive && primitive.isString() && primitive.getAsString().equals("on_shelf")) {
            consumer.accept(Revisions.SINCE_1_21_9);
        }
    }

    @Override
    public @Nullable JsonElement remap(JsonElement element, MinecraftVersion version) {
        if (version.isBelow(MinecraftVersion.V1_21_9) && element instanceof JsonPrimitive primitive && primitive.isString()) {
            if (primitive.getAsString().equals("on_shelf")) {
                return null;
            }
        }
        return element;
    }

    private static class Factory implements SelectPropertyFactory<DisplayContextSelectProperty> {
        @Override
        public DisplayContextSelectProperty create(ConfigSection section) {
            return INSTANCE;
        }
    }

    private static class Reader implements SelectPropertyReader<DisplayContextSelectProperty> {
        @Override
        public DisplayContextSelectProperty read(JsonObject json) {
            return INSTANCE;
        }
    }
}
