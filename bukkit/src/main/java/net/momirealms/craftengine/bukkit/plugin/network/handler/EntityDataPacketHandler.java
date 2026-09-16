package net.momirealms.craftengine.bukkit.plugin.network.handler;

import net.momirealms.craftengine.bukkit.plugin.network.EntityDataValueReplacer;
import net.momirealms.craftengine.bukkit.util.PacketUtils;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.network.EntityPacketHandler;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public final class EntityDataPacketHandler implements EntityPacketHandler {
    public static final EntityDataPacketHandler INSTANCE = new EntityDataPacketHandler();

    private EntityDataPacketHandler() {}

    @Override
    public void handleSetEntityData(Player user, ByteBufPacketEvent event) {
        FriendlyByteBuf buf = event.getBuffer();
        int id = buf.readVarInt();
        List<Object> packedItems = PacketUtils.clientboundSetEntityDataPacket$unpack(buf);
        List<Object> newItems = null;
        for (int i = 0, size = packedItems.size(); i < size; i++) {
            Object newDataValue = EntityDataValueReplacer.replace(user, packedItems.get(i));
            if (newDataValue == null) continue;
            if (newItems == null) newItems = new ArrayList<>(packedItems);
            newItems.set(i, newDataValue);
        }
        if (newItems == null) return;
        event.setChanged(true);
        buf.clear();
        buf.writeVarInt(event.packetID());
        buf.writeVarInt(id);
        PacketUtils.clientboundSetEntityDataPacket$pack(newItems, buf);
    }
}
