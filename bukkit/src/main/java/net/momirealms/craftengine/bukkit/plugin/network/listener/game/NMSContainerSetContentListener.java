package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import net.momirealms.craftengine.bukkit.item.BukkitItem;
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
import net.momirealms.craftengine.proxy.minecraft.core.NonNullListProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundContainerSetContentPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class NMSContainerSetContentListener implements NMSPacketListener {
    public static final NMSContainerSetContentListener INSTANCE = new NMSContainerSetContentListener();

    @Override
    public void onPacketSend(NetWorkUser user, NMSPacketEvent event, Object packet) {
        if (Config.disableItemOperations()) return;
        BukkitServerPlayer serverPlayer = (BukkitServerPlayer) user;
        boolean changed = false;
        List<Object> items = ClientboundContainerSetContentPacketProxy.INSTANCE.getItems(packet);
        int size = items.size();
        @SuppressWarnings("unchecked")
        List<Object> newItems = VersionHelper.isOrAbove1_21_5 ? new ArrayList<>(size) : (List<Object>) NonNullListProxy.INSTANCE.createWithCapacity(size);
        for (int i = 0; i < size; i++) {
            Object raw = items.get(i);
            if (ItemStackProxy.INSTANCE.isEmpty(raw)) {
                newItems.add(raw);
                continue;
            }
            BukkitItem item = ItemStackUtils.wrap(raw);
            Optional<Item> optional = BukkitItemManager.instance().s2c(item.copy(), serverPlayer, ItemPacketSource.CONTAINER);
            if (optional.isPresent()) {
                newItems.add(i, optional.get().minecraftItem());
                changed = true;
            } else {
                newItems.add(i, raw);
            }
        }
        Object rawCarried = ClientboundContainerSetContentPacketProxy.INSTANCE.getCarriedItem(packet);
        Object newCarriedItem = rawCarried;
        Item carriedItem = ItemStackUtils.wrap(rawCarried);
        if (!carriedItem.isEmpty()) {
            Optional<Item> optional = BukkitItemManager.instance().s2c(carriedItem.copy(), serverPlayer, ItemPacketSource.CONTAINER);
            if (optional.isPresent()) {
                changed = true;
                newCarriedItem = optional.get().minecraftItem();
            }
        }
        if (changed) {
            if (VersionHelper.isOrAbove1_21_5) {
                PacketUtils.replacePacket(event, packet, ClientboundContainerSetContentPacketProxy.INSTANCE.newInstance(
                        ClientboundContainerSetContentPacketProxy.INSTANCE.getContainerId(packet),
                        ClientboundContainerSetContentPacketProxy.INSTANCE.getStateId(packet),
                        newItems,
                        newCarriedItem
                ));
            } else {
                PacketUtils.replacePacket(event, packet, ClientboundContainerSetContentPacketProxy.INSTANCE.newInstance$legacy(
                        ClientboundContainerSetContentPacketProxy.INSTANCE.getContainerId(packet),
                        ClientboundContainerSetContentPacketProxy.INSTANCE.getStateId(packet),
                        newItems,
                        newCarriedItem
                ));
            }
        }
    }
}
