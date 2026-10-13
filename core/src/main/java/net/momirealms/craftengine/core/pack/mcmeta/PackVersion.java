package net.momirealms.craftengine.core.pack.mcmeta;

import com.google.gson.JsonArray;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record PackVersion(int major, int minor) implements Comparable<PackVersion> {
    public static final PackVersion MIN_OVERLAY_VERSION = new PackVersion(18, 0); // 1.20.2
    public static final PackVersion PACK_FORMAT_CHANGE_VERSION = new PackVersion(65, 0); // 25w31a
    public static final PackVersion ITEM_ATLAS_VERSION = new PackVersion(73, 0);
    public static final PackVersion PALETTE_TEXTURES_VERSION = new PackVersion(89, 0);
    public static final PackVersion MAX_PACK_VERSION = new PackVersion(1000, 0); // future

    public PackVersion(int major) {
        this(major, 0);
    }

    @Override
    public int compareTo(@NotNull PackVersion o) {
        // 首先比较 major 版本
        int majorCompare = Integer.compare(this.major, o.major);
        if (majorCompare != 0) {
            return majorCompare;
        }
        // 如果 major 相同，则比较 minor 版本
        return Integer.compare(this.minor, o.minor);
    }

    public boolean isAtOrAbove(PackVersion other) {
        return this.compareTo(other) >= 0;
    }

    public boolean isAbove(PackVersion other) {
        return this.compareTo(other) > 0;
    }

    public boolean isAtOrBelow(PackVersion other) {
        return this.compareTo(other) <= 0;
    }

    public boolean isBelow(PackVersion other) {
        return this.compareTo(other) < 0;
    }

    public String asString() {
        if (this.minor == 0) {
            return String.valueOf(this.major);
        } else {
            return this.major + "." + this.minor;
        }
    }

    public JsonArray getAsJsonArray() {
        JsonArray array = new JsonArray();
        array.add(this.major);
        array.add(this.minor);
        return array;
    }

    /**
     * 返回两个版本中较小的那个（版本较低的）
     */
    public static PackVersion getLower(PackVersion v1, PackVersion v2) {
        if (v1 == null) return v2;
        if (v2 == null) return v1;
        return v1.compareTo(v2) <= 0 ? v1 : v2;
    }

    /**
     * 返回两个版本中较大的那个（版本较高的）
     */
    public static PackVersion getHigher(PackVersion v1, PackVersion v2) {
        if (v1 == null) return v2;
        if (v2 == null) return v1;
        return v1.compareTo(v2) >= 0 ? v1 : v2;
    }

    public static PackVersion getLowest(List<PackVersion> versions) {
        if (versions == null || versions.isEmpty()) {
            return null;
        }

        PackVersion lowest = versions.getFirst();
        for (int i = 1; i < versions.size(); i++) {
            lowest = getLower(lowest, versions.get(i));
        }
        return lowest;
    }

    public static PackVersion getHighest(List<PackVersion> versions) {
        if (versions == null || versions.isEmpty()) {
            return null;
        }

        PackVersion highest = versions.getFirst();
        for (int i = 1; i < versions.size(); i++) {
            highest = getHigher(highest, versions.get(i));
        }
        return highest;
    }

    public static PackVersion parse(float num) {
        String str = new BigDecimal(String.valueOf(num)).toPlainString();
        String[] parts = str.split("\\.");
        int integerPart = Integer.parseInt(parts[0]);
        int decimalPart = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        return new PackVersion(integerPart, decimalPart);
    }
}
