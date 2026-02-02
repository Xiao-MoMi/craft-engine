package net.momirealms.craftengine.core.util;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import net.momirealms.craftengine.core.pack.mcmeta.PackVersion;

import java.util.*;

public class MinecraftVersion implements Comparable<MinecraftVersion> {
    private static final Map<Integer, PackVersion> PACK_FORMATS = new HashMap<>();

    static {
        PACK_FORMATS.put(1_16_00, new PackVersion(5, 0));
        PACK_FORMATS.put(1_16_01, new PackVersion(5, 0));
        PACK_FORMATS.put(1_16_02, new PackVersion(6, 0));
        PACK_FORMATS.put(1_16_03, new PackVersion(6, 0));
        PACK_FORMATS.put(1_16_04, new PackVersion(6, 0));
        PACK_FORMATS.put(1_16_05, new PackVersion(6, 0));
        PACK_FORMATS.put(1_17_00, new PackVersion(7, 0));
        PACK_FORMATS.put(1_17_01, new PackVersion(7, 0));
        PACK_FORMATS.put(1_18_00, new PackVersion(8, 0));
        PACK_FORMATS.put(1_18_01, new PackVersion(8, 0));
        PACK_FORMATS.put(1_18_02, new PackVersion(8, 0));
        PACK_FORMATS.put(1_19_00, new PackVersion(9, 0));
        PACK_FORMATS.put(1_19_01, new PackVersion(9, 0));
        PACK_FORMATS.put(1_19_02, new PackVersion(9, 0));
        PACK_FORMATS.put(1_19_03, new PackVersion(12, 0));
        PACK_FORMATS.put(1_19_04, new PackVersion(13, 0));
        PACK_FORMATS.put(1_20_00, new PackVersion(15, 0));
        PACK_FORMATS.put(1_20_01, new PackVersion(15, 0));
        PACK_FORMATS.put(1_20_02, new PackVersion(18, 0));
        PACK_FORMATS.put(1_20_03, new PackVersion(22, 0));
        PACK_FORMATS.put(1_20_04, new PackVersion(22, 0));
        PACK_FORMATS.put(1_20_05, new PackVersion(32, 0));
        PACK_FORMATS.put(1_20_06, new PackVersion(32, 0));
        PACK_FORMATS.put(1_21_00, new PackVersion(34, 0));
        PACK_FORMATS.put(1_21_01, new PackVersion(34, 0));
        PACK_FORMATS.put(1_21_02, new PackVersion(42, 0));
        PACK_FORMATS.put(1_21_03, new PackVersion(42, 0));
        PACK_FORMATS.put(1_21_04, new PackVersion(46, 0));
        PACK_FORMATS.put(1_21_05, new PackVersion(55, 0));
        PACK_FORMATS.put(1_21_06, new PackVersion(63, 0));
        PACK_FORMATS.put(1_21_07, new PackVersion(64, 0));
        PACK_FORMATS.put(1_21_08, new PackVersion(64, 0));
        PACK_FORMATS.put(1_21_09, new PackVersion(69, 0));
        PACK_FORMATS.put(1_21_10, new PackVersion(69, 0));
        PACK_FORMATS.put(1_21_11, new PackVersion(75, 0));
        PACK_FORMATS.put(26_01_00, new PackVersion(76, 0));
        PACK_FORMATS.put(99_99_99, new PackVersion(1000, 0));
    }

    private static final Map<String, MinecraftVersion> BY_NAME = new LinkedHashMap<>();
    private static final Multimap<Integer, MinecraftVersion> BY_PACK_FORMAT = ArrayListMultimap.create();
    public static final MinecraftVersion V1_16 = new MinecraftVersion("1.16");
    public static final MinecraftVersion V1_16_1 = new MinecraftVersion("1.16.1");
    public static final MinecraftVersion V1_16_2 = new MinecraftVersion("1.16.2");
    public static final MinecraftVersion V1_16_3 = new MinecraftVersion("1.16.3");
    public static final MinecraftVersion V1_16_4 = new MinecraftVersion("1.16.4");
    public static final MinecraftVersion V1_16_5 = new MinecraftVersion("1.16.5");
    public static final MinecraftVersion V1_17 = new MinecraftVersion("1.17");
    public static final MinecraftVersion V1_17_1 = new MinecraftVersion("1.17.1");
    public static final MinecraftVersion V1_18 = new MinecraftVersion("1.18");
    public static final MinecraftVersion V1_18_1 = new MinecraftVersion("1.18.1");
    public static final MinecraftVersion V1_18_2 = new MinecraftVersion("1.18.2");
    public static final MinecraftVersion V1_19 = new MinecraftVersion("1.19");
    public static final MinecraftVersion V1_19_1 = new MinecraftVersion("1.19.1");
    public static final MinecraftVersion V1_19_2 = new MinecraftVersion("1.19.2");
    public static final MinecraftVersion V1_19_3 = new MinecraftVersion("1.19.3");
    public static final MinecraftVersion V1_19_4 = new MinecraftVersion("1.19.4");
    public static final MinecraftVersion V1_20 = new MinecraftVersion("1.20");
    public static final MinecraftVersion V1_20_1 = new MinecraftVersion("1.20.1");
    public static final MinecraftVersion V1_20_2 = new MinecraftVersion("1.20.2");
    public static final MinecraftVersion V1_20_3 = new MinecraftVersion("1.20.3");
    public static final MinecraftVersion V1_20_4 = new MinecraftVersion("1.20.4");
    public static final MinecraftVersion V1_20_5 = new MinecraftVersion("1.20.5");
    public static final MinecraftVersion V1_20_6 = new MinecraftVersion("1.20.6");
    public static final MinecraftVersion V1_21 = new MinecraftVersion("1.21");
    public static final MinecraftVersion V1_21_1 = new MinecraftVersion("1.21.1");
    public static final MinecraftVersion V1_21_2 = new MinecraftVersion("1.21.2");
    public static final MinecraftVersion V1_21_3 = new MinecraftVersion("1.21.3");
    public static final MinecraftVersion V1_21_4 = new MinecraftVersion("1.21.4");
    public static final MinecraftVersion V1_21_5 = new MinecraftVersion("1.21.5");
    public static final MinecraftVersion V1_21_6 = new MinecraftVersion("1.21.6");
    public static final MinecraftVersion V1_21_7 = new MinecraftVersion("1.21.7");
    public static final MinecraftVersion V1_21_8 = new MinecraftVersion("1.21.8");
    public static final MinecraftVersion V1_21_9 = new MinecraftVersion("1.21.9");
    public static final MinecraftVersion V1_21_10 = new MinecraftVersion("1.21.10");
    public static final MinecraftVersion V1_21_11 = new MinecraftVersion("1.21.11");
    public static final MinecraftVersion V26_1 = new MinecraftVersion("26.1");
    public static final MinecraftVersion FUTURE = new MinecraftVersion("99.99.99");

    private final int version;
    private final String versionString;
    private final PackVersion packFormat;

    public static MinecraftVersion byName(final String version) {
        MinecraftVersion mcV = BY_NAME.get(version);
        if (mcV == null) {
            throw new IllegalArgumentException("Unsupported version: " + version);
        }
        return mcV;
    }

    public static List<MinecraftVersion> byMajorPackFormat(final int packFormat) {
        List<MinecraftVersion> minecraftVersions = (List<MinecraftVersion>) BY_PACK_FORMAT.get(packFormat);
        if (minecraftVersions.isEmpty()) {
            throw new IllegalArgumentException("Unsupported pack format: " + packFormat);
        }
        return minecraftVersions;
    }

    public String version() {
        return versionString;
    }

    public PackVersion packFormat() {
        return this.packFormat;
    }

    public int majorPackFormat() {
        return packFormat().major();
    }

    private MinecraftVersion(String version) {
        this.version = VersionHelper.parseVersionToInteger(version);
        this.versionString = version;
        this.packFormat = Objects.requireNonNull(PACK_FORMATS.get(this.version), String.valueOf(this.version));
        BY_NAME.put(this.versionString, this);
        BY_PACK_FORMAT.put(this.packFormat.major(), this);
    }

    public boolean isAtOrAbove(MinecraftVersion other) {
        return version >= other.version;
    }

    public boolean isAtOrBelow(MinecraftVersion other) {
        return version <= other.version;
    }

    public boolean is(MinecraftVersion other) {
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
        return this.version;
    }

    @Override
    public int compareTo(MinecraftVersion other) {
        return Integer.compare(this.version, other.version);
    }
}
