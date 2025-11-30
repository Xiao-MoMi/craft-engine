package net.momirealms.craftengine.core.util;

import java.util.HashMap;
import java.util.Map;

public final class MinecraftVersion implements Comparable<MinecraftVersion> {
    public static final Map<Integer, Integer> PACK_FORMATS = new HashMap<>();
    static {
        PACK_FORMATS.put(1_20_00, 15);
        PACK_FORMATS.put(1_20_01, 15);
        PACK_FORMATS.put(1_20_02, 18);
        PACK_FORMATS.put(1_20_03, 22);
        PACK_FORMATS.put(1_20_04, 22);
        PACK_FORMATS.put(1_20_05, 32);
        PACK_FORMATS.put(1_20_06, 32);
        PACK_FORMATS.put(1_21_00, 34);
        PACK_FORMATS.put(1_21_01, 34);
        PACK_FORMATS.put(1_21_02, 42);
        PACK_FORMATS.put(1_21_03, 42);
        PACK_FORMATS.put(1_21_04, 46);
        PACK_FORMATS.put(1_21_05, 55);
        PACK_FORMATS.put(1_21_06, 63);
        PACK_FORMATS.put(1_21_07, 64);
        PACK_FORMATS.put(1_21_08, 64);
        PACK_FORMATS.put(1_21_09, 69);
        PACK_FORMATS.put(1_21_10, 69);
        PACK_FORMATS.put(1_21_11, 75); // todo 确认正式版版本号
        PACK_FORMATS.put(1_99_99, 1000);
    }

    private final int version;
    private final String versionString;
    private final int packFormat;

    public static MinecraftVersion parse(final String version) {
        return new MinecraftVersion(version);
    }

    public String version() {
        return versionString;
    }

    public int packFormat() {
        return packFormat;
    }

    public MinecraftVersion(String version) {
        this.version = VersionHelper.parseVersionToInteger(version);
        this.versionString = version;
        this.packFormat = PACK_FORMATS.get(this.version);
    }

    public boolean isAtOrAbove(MinecraftVersion other) {
        return version >= other.version;
    }

    public boolean isAtOrBelow(MinecraftVersion other) {
        return version <= other.version;
    }

    public boolean isAt(MinecraftVersion other) {
        return version == other.version;
    }

    public boolean isBelow(MinecraftVersion other) {
        return version < other.version;
    }

    public boolean isAbove(MinecraftVersion other) {
        return version > other.version;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof MinecraftVersion that)) return false;
        return version == that.version;
    }

    @Override
    public int hashCode() {
        return version;
    }

    @Override
    public int compareTo(MinecraftVersion other) {
        return Integer.compare(this.version, other.version);
    }
}
