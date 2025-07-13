package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.core.plugin.config.Config;

import java.util.UUID;

public final class ResourcePackUtils {
    private ResourcePackUtils() {}

    public static Object createPacket(UUID uuid, String url, String hash) {
        return FastNMS.INSTANCE.constructor$ClientboundResourcePackPushPacket(uuid, url, hash, Config.kickOnDeclined(), ComponentUtils.adventureToMinecraft(Config.resourcePackPrompt()));
    }

    public static Object createServerResourcePackInfo(UUID uuid, String url, String hash) {
        return FastNMS.INSTANCE.constructor$ServerResourcePackInfo(uuid, url, hash, Config.kickOnDeclined(), ComponentUtils.adventureToMinecraft(Config.resourcePackPrompt()));
    }
}
