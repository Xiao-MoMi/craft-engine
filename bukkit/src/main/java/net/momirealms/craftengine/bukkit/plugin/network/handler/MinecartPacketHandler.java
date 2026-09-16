package net.momirealms.craftengine.bukkit.plugin.network.handler;

import net.momirealms.craftengine.bukkit.entity.data.vehicle.minecart.AbstractMinecartData;
import net.momirealms.craftengine.bukkit.plugin.network.BukkitNetworkManager;
import net.momirealms.craftengine.bukkit.plugin.network.EntityDataValueReplacer;
import net.momirealms.craftengine.bukkit.util.PacketUtils;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.network.EntityPacketHandler;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.proxy.minecraft.network.syncher.SynchedEntityDataProxy;

import java.util.ArrayList;
import java.util.List;

public final class MinecartPacketHandler implements EntityPacketHandler {
    public static final MinecartPacketHandler INSTANCE = new MinecartPacketHandler();
    // 1.21.5 起改为 OPTIONAL_BLOCK_STATE，此字段为 null
    private static final AbstractMinecartData<Integer> DISPLAY_BLOCK_STATE = AbstractMinecartData.DisplayBlockState;

    private MinecartPacketHandler() {}

    @Override
    public void handleSetEntityData(Player user, ByteBufPacketEvent event) {
        FriendlyByteBuf buf = event.getBuffer();
        int id = buf.readVarInt();
        List<Object> packedItems = PacketUtils.clientboundSetEntityDataPacket$unpack(buf);
        List<Object> newItems = null;
        for (int i = 0, size = packedItems.size(); i < size; i++) {
            Object dataValue = packedItems.get(i);
            Object newDataValue = EntityDataValueReplacer.replace(user, dataValue);
            if (newDataValue == null && DISPLAY_BLOCK_STATE != null) {
                newDataValue = replaceDisplayBlockState(user, dataValue);
            }
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

    // 序列化器识别不出来，只能按数据 id 处理
    private static Object replaceDisplayBlockState(Player user, Object dataValue) {
        SynchedEntityDataProxy.DataValueProxy proxy = SynchedEntityDataProxy.DataValueProxy.INSTANCE;
        if (proxy.getId(dataValue) != DISPLAY_BLOCK_STATE.id()) return null;
        int stateId = proxy.getValue(dataValue);
        int newStateId = BukkitNetworkManager.instance().remapBlockState(stateId, user.clientCustomBlockEnabled());
        if (newStateId == stateId) return null;
        return DISPLAY_BLOCK_STATE.createEntityData(newStateId);
    }
}
