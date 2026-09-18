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
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundSetPlayerInventoryPacketProxy;

public final class NMSSetPlayerInventoryListener implements NMSPacketListener {
    public static final NMSSetPlayerInventoryListener INSTANCE = new NMSSetPlayerInventoryListener();

    @Override
    public void onPacketSend(NetWorkUser user, NMSPacketEvent event, Object packet) {
        if (Config.disableItemOperations()) return;
        BukkitServerPlayer serverPlayer = (BukkitServerPlayer) user;
        Item item = ItemStackUtils.wrap(ClientboundSetPlayerInventoryPacketProxy.INSTANCE.getContents(packet));
        BukkitItemManager.instance().s2c(item.copy(), serverPlayer, ItemPacketSource.CONTAINER).ifPresent(newItem -> PacketUtils.replacePacket(event, packet, ClientboundSetPlayerInventoryPacketProxy.INSTANCE.newInstance(
                ClientboundSetPlayerInventoryPacketProxy.INSTANCE.getSlot(packet),
                newItem.minecraftItem()
        )));
    }
}
