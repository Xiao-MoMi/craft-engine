package net.momirealms.craftengine.bedrock.text;

import java.nio.charset.StandardCharsets;

public final class BossBarStyle {
    public static final String PREFIX = "§l§o§r";

    private BossBarStyle() {
    }

    public static String marker(int id) {
        return PREFIX + '§' + Character.forDigit(id / 16, 16) + "§r§" + Character.forDigit(id % 16, 16);
    }

    public static String titleMatches(String marker) {
        // Geyser adds a reset before the title. JSON UI measures its prefix in UTF-8 bytes.
        String prefix = "§r" + marker;
        return "(('%." + prefix.getBytes(StandardCharsets.UTF_8).length + "s' * #bossName) = '" + prefix + "')";
    }
}
