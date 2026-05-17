package net.momirealms.craftengine.core.util;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class VersionHelper {
    private VersionHelper() {}

    public static final boolean IS_RUNNING_IN_DEV = Boolean.getBoolean("net.momirealms.craftengine.dev");
    public static final boolean PREMIUM = false;
    public static final MinecraftVersion MINECRAFT_VERSION;
    public static final boolean COMPONENT_RELEASE;
    public static final int WORLD_VERSION;
    public static final int version;
    public static final int majorVersion;
    public static final int minorVersion;
    public static final boolean isMojmap;
    public static final boolean isFolia;
    public static final boolean isPaper;
    public static final boolean isLeaves;
    public static final boolean isCanvas;
    public static final boolean isLeaf;
    public static final boolean isOrAbove1_20;
    public static final boolean isOrAbove1_20_1;
    public static final boolean isOrAbove1_20_2;
    public static final boolean isOrAbove1_20_3;
    public static final boolean isOrAbove1_20_4;
    public static final boolean isOrAbove1_20_5;
    public static final boolean isOrAbove1_20_6;
    public static final boolean isOrAbove1_21;
    public static final boolean isOrAbove1_21_1;
    public static final boolean isOrAbove1_21_2;
    public static final boolean isOrAbove1_21_3;
    public static final boolean isOrAbove1_21_4;
    public static final boolean isOrAbove1_21_5;
    public static final boolean isOrAbove1_21_6;
    public static final boolean isOrAbove1_21_7;
    public static final boolean isOrAbove1_21_8;
    public static final boolean isOrAbove1_21_9;
    public static final boolean isOrAbove1_21_10;
    public static final boolean isOrAbove1_21_11;
    public static final boolean isOrAbove26_1;
    public static final boolean isOrAbove26_1_1;
    public static final boolean isOrAbove26_1_2;
    public static final boolean isOrAbove26_2;
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

            isOrAbove1_20 = version >= 12000;
            isOrAbove1_20_1 = version >= 12001;
            isOrAbove1_20_2 = version >= 12002;
            isOrAbove1_20_3 = version >= 12003;
            isOrAbove1_20_4 = version >= 12004;
            isOrAbove1_20_5 = version >= 12005;
            isOrAbove1_20_6 = version >= 12006;
            isOrAbove1_21 = version >= 12100;
            isOrAbove1_21_1 = version >= 12101;
            isOrAbove1_21_2 = version >= 12102;
            isOrAbove1_21_3 = version >= 12103;
            isOrAbove1_21_4 = version >= 12104;
            isOrAbove1_21_5 = version >= 12105;
            isOrAbove1_21_6 = version >= 12106;
            isOrAbove1_21_7 = version >= 12107;
            isOrAbove1_21_8 = version >= 12108;
            isOrAbove1_21_9 = version >= 12109;
            isOrAbove1_21_10 = version >= 12110;
            isOrAbove1_21_11 = version >= 12111;
            isOrAbove26_1 = version >= 260100;
            isOrAbove26_1_1 = version >= 260101;
            isOrAbove26_1_2 = version >= 260102;
            isOrAbove26_2 = version >= 260200;

            majorVersion = major;
            minorVersion = minor;

            COMPONENT_RELEASE = isOrAbove1_20_5;

            isMojmap = checkMojMap() || isOrAbove26_1;
            isFolia = checkFolia();
            isPaper = checkPaper();
            isLeaves = checkLeaves();
            isCanvas = checkCanvas();
            isLeaf = checkLeaf();
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
        return 10000 * v1 + v2 * 100 + v3;
    }

    private static boolean exists(String... classNames) {
        for (String className : classNames) {
            try {
                Class.forName(className.replace("{}", "."), false, VersionHelper.class.getClassLoader());
                return true;
            } catch (ClassNotFoundException ignored) {
            }
        }
        return false;
    }

    private static boolean checkMojMap() {
        // Check if the server is Mojmap
        return exists("net.neoforged.art.internal.RenamerImpl");
    }

    private static boolean checkFolia() {
        return exists("io.papermc.paper.threadedregions.RegionizedServer");
    }

    private static boolean checkPaper() {
        return exists("io.papermc.paper.adventure.PaperAdventure");
    }

    private static boolean checkLeaves() {
        return exists("org.leavesmc.leaves.bot.BotList");
    }

    private static boolean checkCanvas() {
        return exists("io.canvasmc.canvas.Config") || exists("io.canvasmc.canvas.GlobalConfiguration");
    }

    private static boolean checkLeaf() {
        return exists("org.dreeam.leaf.config.LeafConfig");
    }

    @Deprecated(forRemoval = true)
    public static int majorVersion() {
        return majorVersion;
    }

    @Deprecated(forRemoval = true)
    public static int minorVersion() {
        return minorVersion;
    }

    @Deprecated(forRemoval = true)
    public static int version() {
        return version;
    }

    @Deprecated(forRemoval = true)
    public static boolean isFolia() {
        return isFolia;
    }

    @Deprecated(forRemoval = true)
    public static boolean isPaper() {
        return isPaper;
    }

    @Deprecated(forRemoval = true)
    public static boolean isCanvas() {
        return isCanvas;
    }

    @Deprecated(forRemoval = true)
    public static boolean isLeaves() {
        return isLeaves;
    }

    @Deprecated(forRemoval = true)
    public static boolean isLeaf() {
        return isLeaf;
    }

    @Deprecated(forRemoval = true)
    public static boolean isMojmap() {
        return isMojmap;
    }

    @Deprecated(forRemoval = true)
    public static boolean isOrAbove1_20() {
        return isOrAbove1_20;
    }

    @Deprecated(forRemoval = true)
    public static boolean isOrAbove1_20_1() {
        return isOrAbove1_20_1;
    }

    @Deprecated(forRemoval = true)
    public static boolean isOrAbove1_20_2() {
        return isOrAbove1_20_2;
    }

    @Deprecated(forRemoval = true)
    public static boolean isOrAbove1_20_3() {
        return isOrAbove1_20_3;
    }

    @Deprecated(forRemoval = true)
    public static boolean isOrAbove1_20_4() {
        return isOrAbove1_20_4;
    }

    @Deprecated(forRemoval = true)
    public static boolean isOrAbove1_20_5() {
        return isOrAbove1_20_5;
    }

    @Deprecated(forRemoval = true)
    public static boolean isOrAbove1_20_6() {
        return isOrAbove1_20_6;
    }

    @Deprecated(forRemoval = true)
    public static boolean isOrAbove1_21() {
        return isOrAbove1_21;
    }

    @Deprecated(forRemoval = true)
    public static boolean isOrAbove1_21_1() {
        return isOrAbove1_21_1;
    }

    @Deprecated(forRemoval = true)
    public static boolean isOrAbove1_21_2() {
        return isOrAbove1_21_2;
    }

    @Deprecated(forRemoval = true)
    public static boolean isOrAbove1_21_3() {
        return isOrAbove1_21_3;
    }

    @Deprecated(forRemoval = true)
    public static boolean isOrAbove1_21_4() {
        return isOrAbove1_21_4;
    }

    @Deprecated(forRemoval = true)
    public static boolean isOrAbove1_21_5() {
        return isOrAbove1_21_5;
    }

    @Deprecated(forRemoval = true)
    public static boolean isOrAbove1_21_6() {
        return isOrAbove1_21_6;
    }

    @Deprecated(forRemoval = true)
    public static boolean isOrAbove1_21_7() {
        return isOrAbove1_21_7;
    }

    @Deprecated(forRemoval = true)
    public static boolean isOrAbove1_21_8() {
        return isOrAbove1_21_8;
    }

    @Deprecated(forRemoval = true)
    public static boolean isOrAbove1_21_9() {
        return isOrAbove1_21_9;
    }

    @Deprecated(forRemoval = true)
    public static boolean isOrAbove1_21_10() {
        return isOrAbove1_21_10;
    }

    @Deprecated(forRemoval = true)
    public static boolean isOrAbove1_21_11() {
        return isOrAbove1_21_11;
    }

    @Deprecated(forRemoval = true)
    public static boolean isOrAbove26_1() {
        return isOrAbove26_1;
    }

    @Deprecated(forRemoval = true)
    public static boolean isOrAbove26_1_1() {
        return isOrAbove26_1_1;
    }

    @Deprecated(forRemoval = true)
    public static boolean isOrAbove26_1_2() {
        return isOrAbove26_1_2;
    }
}