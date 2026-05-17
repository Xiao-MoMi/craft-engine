package net.momirealms.craftengine.core.pack.mcmeta;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import net.momirealms.craftengine.core.util.Pair;

import java.util.ArrayList;
import java.util.List;

public final class Overlay {
    private final PackVersion minVersion;
    private final PackVersion maxVersion;
    private final String directory;

    public Overlay(PackVersion minVersion,
                   PackVersion maxVersion,
                   String directory
    ) {
        this.minVersion = minVersion;
        this.maxVersion = maxVersion;
        this.directory = directory;
    }

    public Overlay(JsonObject overlay) {
        this.directory = overlay.get("directory").getAsString();
        Pair<PackVersion, PackVersion> supportedVersions = getSupportedVersions(overlay);
        this.minVersion = supportedVersions.left();
        this.maxVersion = supportedVersions.right();
    }

    @Override
    public String toString() {
        return "Overlay{" +
                "minVersion=" + minVersion +
                ", maxVersion=" + maxVersion +
                ", directory='" + directory + '\'' +
                '}';
    }

    public JsonObject getAsOverlayEntry(boolean legacy) {
        JsonObject entry = new JsonObject();
        entry.addProperty("directory", this.directory);
        JsonArray minFormat = new JsonArray();
        minFormat.add(new JsonPrimitive(this.minVersion.major()));
        minFormat.add(new JsonPrimitive(this.minVersion.minor()));
        entry.add("min_format", minFormat);
        JsonArray maxFormat = new JsonArray();
        maxFormat.add(new JsonPrimitive(this.maxVersion.major()));
        maxFormat.add(new JsonPrimitive(this.maxVersion.minor()));
        entry.add("max_format", maxFormat);
        if (legacy) {
            JsonArray formats = new JsonArray();
            formats.add(new JsonPrimitive(this.minVersion.major()));
            formats.add(new JsonPrimitive(this.maxVersion.major()));
            entry.add("formats", formats);
        }
        return entry;
    }

    public PackVersion minVersion() {
        return this.minVersion;
    }

    public PackVersion maxVersion() {
        return this.maxVersion;
    }

    public String directory() {
        return this.directory;
    }

    public boolean test(MinecraftVersion version) {
        return version.packFormat().isAtOrAbove(this.minVersion) && version.packFormat().isAtOrBelow(this.maxVersion);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Overlay overlay)) return false;
        return this.minVersion.equals(overlay.minVersion) && this.maxVersion.equals(overlay.maxVersion) && this.directory.equals(overlay.directory);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + this.minVersion.hashCode();
        hash = 31 * hash + this.maxVersion.hashCode();
        hash = 37 * hash + this.directory.hashCode();
        return hash;
    }

    private static Pair<PackVersion, PackVersion> getSupportedVersions(JsonObject pack) {
        List<PackVersion> minVersions = new ArrayList<>();
        List<PackVersion> maxVersions = new ArrayList<>();
        if (pack.has("min_format")) {
            minVersions.add(getFormatVersion(pack.get("min_format")));
        }
        if (pack.has("max_format")) {
            maxVersions.add(getFormatVersion(pack.get("max_format")));
        }
        if (pack.has("formats")) {
            Pair<PackVersion, PackVersion> supportedFormats = parseSupportedFormats(pack.get("formats"));
            minVersions.add(supportedFormats.left());
            maxVersions.add(supportedFormats.right());
        }
        if (maxVersions.isEmpty()) {
            maxVersions.add(PackVersion.MAX_PACK_VERSION);
        }
        if (minVersions.isEmpty()) {
            minVersions.add(PackVersion.MIN_OVERLAY_VERSION);
        }
        return Pair.of(
                PackVersion.getLowest(minVersions),
                PackVersion.getHighest(maxVersions)
        );
    }

    private static PackVersion getFormatVersion(JsonElement format) {
        if (format instanceof JsonArray array) {
            if (array.size() == 1) {
                return new PackVersion(GsonHelper.getAsInt(array.get(0), 15), 0);
            }
            if (array.size() == 2) {
                return new PackVersion(GsonHelper.getAsInt(array.get(0), 15), GsonHelper.getAsInt(array.get(1), 1000));
            }
        } else if (format instanceof JsonPrimitive jsonPrimitive) {
            float version = jsonPrimitive.getAsFloat();
            return PackVersion.parse(version);
        }
        throw new IllegalArgumentException("Unknown overlay version format: " + format);
    }

    private static Pair<PackVersion, PackVersion> parseSupportedFormats(JsonElement formats) {
        switch (formats) {
            case JsonPrimitive jsonPrimitive -> {
                return new Pair<>(new PackVersion(jsonPrimitive.getAsInt(), 0), new PackVersion(jsonPrimitive.getAsInt(), 0));
            }
            case JsonArray array -> {
                if (array.size() == 2) {
                    return new Pair<>(new PackVersion(GsonHelper.getAsInt(array.get(0), 15), 0), new PackVersion(GsonHelper.getAsInt(array.get(1), 1000), 0));
                }
            }
            case JsonObject object -> {
                int min = GsonHelper.getAsInt(object.get("min_inclusive"), 15);
                int max = GsonHelper.getAsInt(object.get("max_inclusive"), 1000);
                return new Pair<>(new PackVersion(min, 0), new PackVersion(max, 0));
            }
            default -> {
            }
        }
        throw new IllegalArgumentException("Unsupported overlay version format: " + formats);
    }
}
