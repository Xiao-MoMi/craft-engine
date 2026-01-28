package net.momirealms.craftengine.core.util;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class VersionHelper {
    public static final boolean IS_RUNNING_IN_DEV = Boolean.getBoolean("net.momirealms.craftengine.dev");
    public static final boolean PREMIUM = false;
    public static final MinecraftVersion MINECRAFT_VERSION;
    public static final boolean COMPONENT_RELEASE;
    public static final int WORLD_VERSION;
    private static final int version;
    private static final int majorVersion;
    private static final int minorVersion;
    private static final boolean mojmap;
    private static final boolean folia;
    private static final boolean paper;
    private static final boolean leaves;
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
    private static final boolean v1_21_8;
    private static final boolean v1_21_9;
    private static final boolean v1_21_10;
    private static final boolean v1_21_11;
    private static final boolean v26_1;
    private static final Class<?> UNOBFUSCATED_CLAZZ = Objects.requireNonNull(ReflectionUtils.getClazz(
            "net.minecraft.obfuscate.DontObfuscate", // 因为无混淆版本没有这个类所以说多写几个防止找不到了
            "net.minecraft.data.Main",
            "net.minecraft.server.Main",
            "net.minecraft.gametest.Main",
            "net.minecraft.client.main.Main",
            "net.minecraft.client.data.Main"
    ));

    static {
        try (InputStream inputStream = UNOBFUSCATED_CLAZZ.getResourceAsStream("/version.json")) {
            if (inputStream == null) {
                throw new IOException("Failed to load version.json");
            }
            JsonObject json = GsonHelper.parseJsonToJsonObject(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
            WORLD_VERSION = GsonHelper.getAsInt(json.get("world_version"), -1);
            if (WORLD_VERSION == -1) {
                throw new IllegalStateException("Failed to get world_version from version.json");
            }
            String versionString = json.getAsJsonPrimitive("id").getAsString()
                    .split("-", 2)[0]  // 1.21.10-rc1          -> 1.21.10
                    .split("_", 2)[0]; // 1.21.11_unobfuscated -> 1.21.11

            MINECRAFT_VERSION = MinecraftVersion.byName(versionString);

            String[] split = versionString.split("\\.");
            int major = Integer.parseInt(split[1]);
            int minor = split.length == 3 ? Integer.parseInt(split[2]) : 0;

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
            v1_21_8 = version >= 12108;
            v1_21_9 = version >= 12109;
            v1_21_10 = version >= 12110;
            v1_21_11 = version >= 12111;
            v26_1 = version >= 12601;

            majorVersion = major;
            minorVersion = minor;

            COMPONENT_RELEASE = v1_20_5;

            mojmap = checkMojMap() || v26_1;
            folia = checkFolia();
            paper = checkPaper();
            leaves = checkLeaves();
        } catch (Exception e) {
            throw new RuntimeException("Failed to init VersionHelper", e);
        }
    }

    public static int parseVersionToInteger(String versionString) {
        int v1 = 0;
        int v2 = 0;
        int v3 = 0;
        int currentNumber = 0;
        int part = 0;
        for (int i = 0; i < versionString.length(); i++) {
            char c = versionString.charAt(i);
            if (c >= '0' && c <= '9') {
                currentNumber = currentNumber * 10 + (c - '0');
            } else if (c == '.') {
                if (part == 0) {
                    v1 = currentNumber;
                }
                if (part == 1) {
                    v2 = currentNumber;
                }
                part++;
                currentNumber = 0;
                if (part > 2) {
                    break;
                }
            }
        }
        // 处理最后一个数字部分
        if (part == 0) {  // 没有点号：如 "26"
            v1 = currentNumber;
        } else if (part == 1) {  // 一个点号：如 "26.1"
            v2 = currentNumber;
        } else if (part == 2) {  // 两个点号：如 "1.2.3"
            v3 = currentNumber;
        }

        // 新版命名法
        if (v1 >= 26) {
            return 10000 + v1 * 100 + v2;
        }
        // 旧版命名法
        else {
            return 10000 + v2 * 100 + v3;
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

    private static boolean checkLeaves() {
        try {
            Class.forName("org.leavesmc.leaves.bot.ServerBot");
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

    public static boolean isLeaves() {
        return leaves;
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

    public static boolean isOrAbove1_21_8() {
        return v1_21_8;
    }

    public static boolean isOrAbove1_21_9() {
        return v1_21_9;
    }

    public static boolean isOrAbove1_21_10() {
        return v1_21_10;
    }

    public static boolean isOrAbove1_21_11() {
        return v1_21_11;
    }

    public static boolean isOrAbove26_1() {
        return v26_1;
    }
}