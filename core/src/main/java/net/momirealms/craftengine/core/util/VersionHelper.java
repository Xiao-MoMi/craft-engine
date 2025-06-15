package net.momirealms.craftengine.core.util;

import java.lang.reflect.Field;

import static java.util.Objects.requireNonNull;

public class VersionHelper {
    // todo 在跨平台时候，将其设计到平台实现
    private static final Class<?> clazz$SharedConstants = requireNonNull(ReflectionUtils.getClazz("net.minecraft.SharedConstants"));
    private static final Field field$SharedConstants$VERSION_STRING = requireNonNull(ReflectionUtils.getDeclaredField(clazz$SharedConstants, String.class, 1));

    private static final int version;
    private static final int majorVersion;
    private static final int minorVersion;
    private static final boolean mojmap;
    private static final boolean folia;
    private static final boolean paper;

    private static final boolean v1_20;
    private static final boolean v1_20_1;
    private static final boolean v1_20_2;
    private static final boolean v1_20_3;
    private static final boolean v1_20_4;
    private static final boolean v1_20_5;
    private static final boolean v1_20_6;
    private static final boolean v1_21;
    private static final boolean v1_21_1;
    private static final boolean v1_21_2;
    private static final boolean v1_21_3;
    private static final boolean v1_21_4;
    private static final boolean v1_21_5;
    private static final boolean v1_21_6;

    static {
        try {
            String versionString = (String) field$SharedConstants$VERSION_STRING.get(null);
            String[] split = versionString.split("\\.");
            int major = Integer.parseInt(split[1]);
            int minor = split.length == 3 ? Integer.parseInt(split[2].split("-", 2)[0]) : 0;

            // 2001 = 1.20.1
            // 2104 = 1.21.4
            version = major * 100 + minor;

            v1_20 = version >= 2000;
            v1_20_1 = version >= 2001;
            v1_20_2 = version >= 2002;
            v1_20_3 = version >= 2003;
            v1_20_4 = version >= 2004;
            v1_20_5 = version >= 2005;
            v1_20_6 = version >= 2006;
            v1_21 = version >= 2100;
            v1_21_1 = version >= 2101;
            v1_21_2 = version >= 2102;
            v1_21_3 = version >= 2103;
            v1_21_4 = version >= 2104;
            v1_21_5 = version >= 2105;
            v1_21_6 = version >= 2106;

            majorVersion = major;
            minorVersion = minor;

            mojmap = checkMojMap();
            folia = checkFolia();
            paper = checkPaper();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to init VersionHelper", e);
        }
    }

    public static int majorVersion() {
        return majorVersion;
    }

    public static int minorVersion() {
        return minorVersion;
    }

    public static int version() {
        return version;
    }

    private static boolean checkMojMap() {
        // Check if the server is Mojmap
        try {
            Class.forName("net.neoforged.art.internal.RenamerImpl");
            return true;
        } catch (ClassNotFoundException ignored) {
        }
        return false;
    }

    private static boolean checkFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException ignored) {
        }
        return false;
    }

    private static boolean checkPaper() {
        try {
            Class.forName("io.papermc.paper.adventure.PaperAdventure");
            return true;
        } catch (ClassNotFoundException ignored) {
        }
        return false;
    }

    public static boolean isFolia() {
        return folia;
    }

    public static boolean isPaper() {
        return paper;
    }

    public static boolean isMojmap() {
        return mojmap;
    }

    public static boolean isOrAbove1_20() {
        return v1_20;
    }

    public static boolean isOrAbove1_20_1() {
        return v1_20_1;
    }

    public static boolean isOrAbove1_20_2() {
        return v1_20_2;
    }

    public static boolean isOrAbove1_20_3() {
        return v1_20_3;
    }

    public static boolean isOrAbove1_20_4() {
        return v1_20_4;
    }

    public static boolean isOrAbove1_20_5() {
        return v1_20_5;
    }

    public static boolean isOrAbove1_20_6() {
        return v1_20_6;
    }

    public static boolean isOrAbove1_21() {
        return v1_21;
    }

    public static boolean isOrAbove1_21_1() {
        return v1_21_1;
    }

    public static boolean isOrAbove1_21_2() {
        return v1_21_2;
    }

    public static boolean isOrAbove1_21_3() {
        return v1_21_3;
    }

    public static boolean isOrAbove1_21_4() {
        return v1_21_4;
    }

    public static boolean isOrAbove1_21_5() {
        return v1_21_5;
    }

    public static boolean isOrAbove1_21_6() {
        return v1_21_6;
    }
}