package net.momirealms.craftengine.core.pack.atlas;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record UnstitchSource(Key resource,
                             double divisorX,
                             double divisorY,
                             List<Region> regions) implements SpriteSource {
    private static final String[] DIVISOR_X = ConfigKeys.of("divisor_x");
    private static final String[] DIVISOR_Y = ConfigKeys.of("divisor_y");
    private static final String[] REGIONS = ConfigKeys.of("region(s)");

    public UnstitchSource {
        regions = List.copyOf(regions);
    }

    static @Nullable UnstitchSource fromJson(JsonObject json) {
        if (!json.has("resource")) return null;
        Key resource = Key.of(json.get("resource").getAsString());
        double divisorX = json.has("divisor_x") ? json.get("divisor_x").getAsDouble() : 1d;
        double divisorY = json.has("divisor_y") ? json.get("divisor_y").getAsDouble() : 1d;
        List<Region> regions = new ArrayList<>();
        JsonArray regionsJson = json.getAsJsonArray("regions");
        if (regionsJson != null) {
            for (JsonElement element : regionsJson) {
                if (!(element instanceof JsonObject regionJson) || !regionJson.has("sprite")) continue;
                regions.add(new Region(
                        Key.of(regionJson.get("sprite").getAsString()),
                        regionJson.has("x") ? regionJson.get("x").getAsDouble() : 0d,
                        regionJson.has("y") ? regionJson.get("y").getAsDouble() : 0d,
                        regionJson.has("width") ? regionJson.get("width").getAsDouble() : 0d,
                        regionJson.has("height") ? regionJson.get("height").getAsDouble() : 0d
                ));
            }
        }
        return new UnstitchSource(resource, divisorX, divisorY, regions);
    }

    static UnstitchSource fromConfig(ConfigSection config) {
        Key resource = config.getNonNullKey("resource");
        double divisorX = config.getDouble(DIVISOR_X, 1d);
        double divisorY = config.getDouble(DIVISOR_Y, 1d);
        List<Region> regions = config.getList(REGIONS, value -> {
            ConfigSection region = value.getAsSection();
            return new Region(
                    region.getNonNullKey("sprite"),
                    region.getDouble("x", 0d),
                    region.getDouble("y", 0d),
                    region.getDouble("width", 0d),
                    region.getDouble("height", 0d)
            );
        });
        return new UnstitchSource(resource, divisorX, divisorY, regions);
    }

    @Override
    public JsonObject get() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "unstitch");
        json.addProperty("resource", this.resource.toString());
        json.addProperty("divisor_x", this.divisorX);
        json.addProperty("divisor_y", this.divisorY);
        JsonArray regions = new JsonArray();
        for (Region region : this.regions) {
            JsonObject regionJson = new JsonObject();
            regionJson.addProperty("sprite", region.sprite().toString());
            regionJson.addProperty("x", region.x());
            regionJson.addProperty("y", region.y());
            regionJson.addProperty("width", region.width());
            regionJson.addProperty("height", region.height());
            regions.add(regionJson);
        }
        json.add("regions", regions);
        return json;
    }

    public record Region(Key sprite, double x, double y, double width, double height) {
    }
}
