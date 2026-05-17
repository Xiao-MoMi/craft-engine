package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.PacketUtils;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ContainerSetContentListener implements ByteBufferPacketListener {
    public static final ByteBufferPacketListener INSTANCE = new ContainerSetContentListener();

    private ContainerSetContentListener() {}

    @Override
    public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
        if (Config.disableItemOperations()) return;
        if (!(user instanceof BukkitServerPlayer serverPlayer)) return;
        FriendlyByteBuf buf = event.getBuffer();
        int containerId = buf.readContainerId();
        int stateId = buf.readVarInt();
        int listSize = buf.readVarInt();
        List<Item> items = new ArrayList<>(listSize);
        boolean changed = false;
        for (int i = 0; i < listSize; i++) {
            Item item = PacketUtils.readItem(buf);
            Optional<Item> optional = BukkitItemManager.instance().s2c(item, serverPlayer);
            if (optional.isPresent()) {
                items.add(optional.get());
                changed = true;
            } else {
                items.add(item);
            }
        }
        Item carriedItem = PacketUtils.readItem(buf);
        Item newCarriedItem = carriedItem;
        Optional<Item> optional = BukkitItemManager.instance().s2c(carriedItem, serverPlayer);
        if (optional.isPresent()) {
            changed = true;
            newCarriedItem = optional.get();
        }
        if (!changed) return;
        event.setChanged(true);
        buf.clear();
        buf.writeVarInt(event.packetID());
        buf.writeContainerId(containerId);
        buf.writeVarInt(stateId);
        buf.writeVarInt(listSize);
        for (Item itemStack : items) {
            PacketUtils.writeItem(buf, itemStack);
        }
        PacketUtils.writeItem(buf, newCarriedItem);
    }
}
