package net.momirealms.craftengine.core.util;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class VersionHelper {
    public static final MinecraftVersion MINECRAFT_VERSION;
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
    private static final boolean v1_21_7;

    static {
        try (InputStream inputStream = Class.forName("net.minecraft.obfuscate.DontObfuscate").getResourceAsStream("/version.json")) {
            if (inputStream == null) {
                throw new IOException("Failed to load version.json");
            }
            JsonObject json = GsonHelper.parseJsonToJsonObject(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
            String versionString = json.getAsJsonPrimitive("id").getAsString().split("-", 2)[0];

            MINECRAFT_VERSION = new MinecraftVersion(versionString);

            String[] split = versionString.split("\\.");
            int major = Integer.parseInt(split[1]);
            int minor = split.length == 3 ? Integer.parseInt(split[2].split("-", 2)[0]) : 0;

            // 12001 = 1.20.1
            // 12104 = 1.21.4
            version = parseVersionToInteger(versionString);

            v1_20 = version >= 12000;
            v1_20_1 = version >= 12001;
            v1_20_2 = version >= 12002;
            v1_20_3 = version >= 12003;
            v1_20_4 = version >= 12004;
            v1_20_5 = version >= 12005;
            v1_20_6 = version >= 12006;
            v1_21 = version >= 12100;
            v1_21_1 = version >= 12101;
            v1_21_2 = version >= 12102;
            v1_21_3 = version >= 12103;
            v1_21_4 = version >= 12104;
            v1_21_5 = version >= 12105;
            v1_21_6 = version >= 12106;
            v1_21_7 = version >= 12107;

            majorVersion = major;
            minorVersion = minor;

            mojmap = checkMojMap();
            folia = checkFolia();
            paper = checkPaper();
        } catch (Exception e) {
            throw new RuntimeException("Failed to init VersionHelper", e);
        }
    }

    public static int parseVersionToInteger(String versionString) {
        int major = 0;
        int minor = 0;
        int currentNumber = 0;
        int part = 0;
        for (int i = 0; i < versionString.length(); i++) {
            char c = versionString.charAt(i);
            if (c >= '0' && c <= '9') {
                currentNumber = currentNumber * 10 + (c - '0');
            } else if (c == '.') {
                if (part == 1) {
                    major = currentNumber;
                }
                part++;
                currentNumber = 0;
                if (part > 2) {
                    break;
                }
            }
        }
        if (part == 1) {
            major = currentNumber;
        } else if (part == 2) {
            minor = currentNumber;
        }
        return 10000 + major * 100 + minor;
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

    public static boolean isOrAbove1_21_7() {
        return v1_21_7;
    }
}