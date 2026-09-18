package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.bukkit.util.PacketUtils;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.network.ItemPacketSource;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.NMSPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.NMSPacketListener;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundSetCursorItemPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.player.PlayerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.inventory.AbstractContainerMenuProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;

public final class NMSSetCursorItemListener implements NMSPacketListener {
    public static final NMSSetCursorItemListener INSTANCE = new NMSSetCursorItemListener();

    @Override
    public void onPacketSend(NetWorkUser user, NMSPacketEvent event, Object packet) {
        if (Config.disableItemOperations()) return;
        BukkitServerPlayer serverPlayer = (BukkitServerPlayer) user;
        Item item = ItemStackUtils.wrap(ClientboundSetCursorItemPacketProxy.INSTANCE.getContents(packet));

        // 为了避免其他插件造成的手感冲突
        if (VersionHelper.isOrAbove1_21_5) {
            // 发出来的是非空物品
            if (!item.isEmpty()) {
                Object containerMenu = PlayerProxy.INSTANCE.getContainerMenu(serverPlayer.minecraftPlayer());
                if (containerMenu != null) {
                    Object actualItem = AbstractContainerMenuProxy.INSTANCE.getCarried(containerMenu);
                    // 但服务端上实际确是空气，就把它写成空气，避免因为其他插件导致手感问题
                    if (ItemStackProxy.INSTANCE.isEmpty(actualItem)) {
                        PacketUtils.replacePacket(event, packet, (ClientboundSetCursorItemPacketProxy.INSTANCE.newInstance(actualItem)));
                        return;
                    }
                }
            }
        }

        BukkitItemManager.instance().s2c(item.copy(), serverPlayer, ItemPacketSource.CONTAINER).ifPresent(newItem -> {
            PacketUtils.replacePacket(event, packet, ClientboundSetCursorItemPacketProxy.INSTANCE.newInstance(newItem.minecraftItem()));
        });
    }
}
