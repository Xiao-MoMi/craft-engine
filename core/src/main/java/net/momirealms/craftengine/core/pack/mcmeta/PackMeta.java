package net.momirealms.craftengine.core.pack.mcmeta;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

public final class PackMeta {
    private final JsonElement description;
    @ApiStatus.Obsolete
    @Nullable
    private final Integer packFormat;
    private final PackVersion minVersion;
    private final PackVersion maxVersion;
    private final Overlays overlays;
    @Nullable
    private final PackFeatures features;
    @Nullable
    private final PackFilter filter;
    @Nullable
    private final PackLanguage language;

    public PackMeta(JsonObject mcmeta) {
        JsonObject pack = mcmeta.getAsJsonObject("pack");
        this.description = pack.get("description");
        this.packFormat = pack.has("pack_format") ? pack.get("pack_format").getAsInt() : null;
        if (pack.has("min_format") || pack.has("max_format")) {
            this.minVersion = this.readVersion(pack.get("min_format"), 0);
            this.maxVersion = this.readVersion(pack.get("max_format"), Integer.MAX_VALUE);
        } else {
            JsonElement supported = pack.get("supported_formats");
            int min;
            int max;
            if (supported instanceof JsonPrimitive primitive) {
                min = primitive.getAsInt();
                max = min;
            } else if (supported instanceof JsonArray array && array.size() == 2) {
                min = array.get(0).getAsInt();
                max = array.get(1).getAsInt();
            } else if (supported instanceof JsonObject range) {
                min = range.get("min_inclusive").getAsInt();
                max = range.get("max_inclusive").getAsInt();
            } else if (supported == null && this.packFormat != null) {
                min = this.packFormat;
                max = min;
            } else {
                throw new IllegalArgumentException("Missing or invalid pack format range in pack.mcmeta");
            }
            this.minVersion = new PackVersion(min, 0);
            this.maxVersion = new PackVersion(max, Integer.MAX_VALUE);
        }
        if (this.minVersion.isAbove(this.maxVersion)) {
            throw new IllegalArgumentException("Minimum pack format exceeds maximum pack format");
        }
        this.overlays = new Overlays(mcmeta);
        this.features = mcmeta.has("features") ? new PackFeatures(mcmeta.getAsJsonObject("features")) : null;
        this.filter = mcmeta.has("filter") ? new PackFilter(mcmeta.getAsJsonObject("filter")) : null;
        this.language = mcmeta.has("language") ? new PackLanguage(mcmeta.getAsJsonObject("language")) : null;
    }

    public Overlays overlays() {
        return this.overlays;
    }

    public JsonElement description() {
        return this.description;
    }

    @ApiStatus.Obsolete
    @Nullable
    public Integer packFormat() {
        return this.packFormat;
    }

    public PackVersion minVersion() {
        return this.minVersion;
    }

    public PackVersion maxVersion() {
        return this.maxVersion;
    }

    @Nullable
    public PackFeatures features() {
        return this.features;
    }

    @Nullable
    public PackFilter filter() {
        return this.filter;
    }

    @Nullable
    public PackLanguage language() {
        return this.language;
    }

    private PackVersion readVersion(JsonElement format, int defaultMinor) {
        if (format instanceof JsonPrimitive primitive) {
            return new PackVersion(primitive.getAsInt(), defaultMinor);
        }
        if (format instanceof JsonArray array) {
            if (array.size() == 1) {
                return new PackVersion(array.get(0).getAsInt(), defaultMinor);
            }
            if (array.size() == 2) {
                return new PackVersion(array.get(0).getAsInt(), array.get(1).getAsInt());
            }
        }
        throw new IllegalArgumentException("Missing or invalid pack format version: " + format);
    }
}
