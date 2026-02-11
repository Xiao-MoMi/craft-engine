package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.common.ClientboundResourcePackPushPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.server.MinecraftServerProxy;

import java.util.Optional;
import java.util.UUID;

public final class ResourcePackUtils {
    private ResourcePackUtils() {}

    public static Object createPacket(UUID uuid, String url, String hash) {
        if (VersionHelper.isOrAbove1_20_5()) {
            return ClientboundResourcePackPushPacketProxy.INSTANCE.newInstance(
                    uuid, url, hash, Config.kickOnDeclined(),
                    Optional.ofNullable(ComponentUtils.adventureToMinecraft(Config.resourcePackPrompt()))
            );
        } else if (VersionHelper.isOrAbove1_20_3()) {
            return ClientboundResourcePackPushPacketProxy.INSTANCE.newInstance(
                    uuid, url, hash, Config.kickOnDeclined(),
                    ComponentUtils.adventureToMinecraft(Config.resourcePackPrompt())
            );
        } else {
            return ClientboundResourcePackPushPacketProxy.INSTANCE.newInstance(
                    url, hash, Config.kickOnDeclined(),
                    ComponentUtils.adventureToMinecraft(Config.resourcePackPrompt())
            );
        }
    }

    public static Object createServerResourcePackInfo(UUID uuid, String url, String hash) {
        if (VersionHelper.isOrAbove1_20_3()) {
            return MinecraftServerProxy.ServerResourcePackInfoProxy.INSTANCE.newInstance(uuid, url, hash, Config.kickOnDeclined(), ComponentUtils.adventureToMinecraft(Config.resourcePackPrompt()));
        } else {
            return MinecraftServerProxy.ServerResourcePackInfoProxy.INSTANCE.newInstance(url, hash, Config.kickOnDeclined(), ComponentUtils.adventureToMinecraft(Config.resourcePackPrompt()));
        }
    }
}
