package net.momirealms.craftengine.core.util;

public class VersionHelper {
    private static float version;
    private static boolean mojmap;
    private static boolean folia;
    private static boolean paper;

    public static void init(String serverVersion) {
        String[] split = serverVersion.split("\\.");
        version = Float.parseFloat(split[1] + "." + (split.length == 3 ? split[2] : "0"));
        checkMojMap();
        checkFolia();
        checkPaper();
    }

    public static float version() {
        return version;
    }

    private static void checkMojMap() {
        // Check if the server is Mojmap
        try {
            Class.forName("net.minecraft.network.protocol.game.ClientboundBossEventPacket");
            mojmap = true;
        } catch (ClassNotFoundException ignored) {
        }
    }

    private static void checkFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException ignored) {
        }
    }

    private static void checkPaper() {
        try {
            Class.forName("io.papermc.paper.adventure.PaperAdventure");
            paper = true;
        } catch (ClassNotFoundException ignored) {
        }
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

    public static boolean isVersionNewerThan1_20() {
        return version >= 20f;
    }

    public static boolean isVersionNewerThan1_20_2() {
        return version >= 20.19f;
    }

    public static boolean isVersionNewerThan1_20_3() {
        return version >= 20.29f;
    }

    public static boolean isVersionNewerThan1_20_4() {
        return version >= 20.39f;
    }

    public static boolean isVersionNewerThan1_20_5() {
        return version >= 20.49f;
    }

    public static boolean isVersionNewerThan1_21() {
        return version >= 21f;
    }

    public static boolean isVersionNewerThan1_21_2() {
        return version >= 21.19f;
    }

    public static boolean isVersionNewerThan1_21_3() {
        return version >= 21.29f;
    }

    public static boolean isVersionNewerThan1_21_4() {
        return version >= 21.39f;
    }

    public static boolean isVersionNewerThan1_21_5() {
        return version >= 21.49f;
    }
}