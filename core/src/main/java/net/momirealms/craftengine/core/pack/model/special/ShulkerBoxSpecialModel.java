package net.momirealms.craftengine.core.pack.model.special;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.revision.Revision;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class ShulkerBoxSpecialModel implements SpecialModel {
    public static final Factory FACTORY = new Factory();
    public static final Reader READER = new Reader();
    private final String texture;
    private final float openness;
    private final Direction orientation;

    public ShulkerBoxSpecialModel(String texture, float openness, @Nullable Direction orientation) {
        this.texture = texture;
        this.openness = openness;
        this.orientation = orientation;
    }

    @Override
    public List<Revision> revisions() {
        return List.of();
    }

    @Override
    public Key type() {
        return SpecialModels.SHULKER_BOX;
    }

    @Override
    public JsonObject apply(MinecraftVersion version) {
        JsonObject json = new JsonObject();
        json.addProperty("type", type().toString());
        json.addProperty("texture", texture);
        if (orientation != null) {
            json.addProperty("orientation", orientation.name().toLowerCase(Locale.ENGLISH));
        }
        json.addProperty("openness", openness);
        return json;
    }

    public static class Factory implements SpecialModelFactory {
        @Override
        public SpecialModel create(Map<String, Object> arguments) {
            float openness = ResourceConfigUtils.getAsFloat(arguments.getOrDefault("openness", 0), "openness");
            String texture = ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("texture"), "warning.config.item.model.special.shulker_box.missing_texture");
            Direction orientation = Optional.ofNullable(arguments.get("orientation")).map(String::valueOf).map(String::toUpperCase).map(Direction::valueOf).orElse(null);
            if (openness > 1 || openness < 0) {
                throw new LocalizedResourceConfigException("warning.config.item.model.special.shulker_box.invalid_openness", String.valueOf(openness));
            }
            return new ShulkerBoxSpecialModel(texture, openness, orientation);
        }
    }

    public static class Reader implements SpecialModelReader {
        @Override
        public SpecialModel read(JsonObject json) {
            float openness = json.has("openness") ? json.get("openness").getAsFloat() : 0f;
            Direction orientation = json.has("orientation") ? Direction.valueOf(json.get("orientation").getAsString().toUpperCase(Locale.ENGLISH)) : null;
            String texture = json.get("texture").getAsString();
            return new ShulkerBoxSpecialModel(texture, openness, orientation);
        }
    }
}
