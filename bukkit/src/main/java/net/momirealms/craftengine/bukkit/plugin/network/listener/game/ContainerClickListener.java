package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.util.PacketUtils;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.VersionHelper;

import java.util.Map;
import java.util.Optional;

public final class ContainerClickListener implements ByteBufferPacketListener {
    public static final ByteBufferPacketListener INSTANCE = VersionHelper.isOrAbove1_21_5 ? null : new ContainerClickListener();

    private ContainerClickListener() {}

    @Override
    public void onPacketReceive(NetWorkUser user, ByteBufPacketEvent event) {
        if (Config.disableItemOperations()) return;
        if (!VersionHelper.PREMIUM && !Config.interceptItem()) return;
        FriendlyByteBuf buf = event.getBuffer();
        boolean changed = false;
        int containerId = buf.readContainerId();
        int stateId = buf.readVarInt();
        short slotNum = buf.readShort();
        byte buttonNum = buf.readByte();
        int clickType = buf.readVarInt();
        int i = buf.readVarInt();
        Int2ObjectMap<Item> changedSlots = new Int2ObjectOpenHashMap<>(i);
        for (int j = 0; j < i; ++j) {
            int k = buf.readShort();
            Item item = PacketUtils.readItem(buf);
            Optional<Item> optional = BukkitItemManager.instance().c2s(item);
            if (optional.isPresent()) {
                changed = true;
                item = optional.get();
            }
            changedSlots.put(k, item);
        }
        Item carriedItem = PacketUtils.readItem(buf);
        Optional<Item> optional = BukkitItemManager.instance().c2s(carriedItem);
        if (optional.isPresent()) {
            changed = true;
            carriedItem = optional.get();
        }
        if (changed) {
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeContainerId(containerId);
            buf.writeVarInt(stateId);
            buf.writeShort(slotNum);
            buf.writeByte(buttonNum);
            buf.writeVarInt(clickType);
            buf.writeVarInt(changedSlots.size());
            for (Map.Entry<Integer, Item> entry : changedSlots.int2ObjectEntrySet()) {
                buf.writeShort(entry.getKey());
                PacketUtils.writeItem(buf, entry.getValue());
            }
            PacketUtils.writeItem(buf, carriedItem);
        }
    }
}
