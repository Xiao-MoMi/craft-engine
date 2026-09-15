package net.momirealms.craftengine.bukkit.plugin.network.handler;

import it.unimi.dsi.fastutil.ints.IntList;
import net.momirealms.craftengine.bukkit.plugin.network.EquipmentLodTracker;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.network.PacketPosition;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.VersionHelper;

import java.util.Optional;

public class EquipmentEntityPacketHandler extends EquipmentPacketHandler {
    public static final EquipmentEntityPacketHandler INSTANCE = new EquipmentEntityPacketHandler();

    protected EquipmentEntityPacketHandler() {}

    @Override
    public void handleAddEntity(Player user, ByteBufPacketEvent event, int entityId, FriendlyByteBuf buf) {
        if (!VersionHelper.isOrAbove1_21_2) return;
        EquipmentLodTracker tracker = ((BukkitServerPlayer) user).equipmentLod();
        if (tracker == null) return;
        buf.readUUID();
        buf.readVarInt();
        tracker.add(entityId, new PacketPosition(buf.readDouble(), buf.readDouble(), buf.readDouble()));
    }

    @Override
    public boolean handleEntitiesRemove(Player user, int entityId, IntList entityIds) {
        EquipmentLodTracker tracker = ((BukkitServerPlayer) user).equipmentLod();
        if (tracker != null) tracker.remove(entityId);
        return false;
    }

    @Override
    public void handleMove(Player user, ByteBufPacketEvent event, int entityId, FriendlyByteBuf buf) {
        EquipmentLodTracker tracker = ((BukkitServerPlayer) user).equipmentLod();
        if (tracker != null) tracker.move(entityId, buf.readShort(), buf.readShort(), buf.readShort());
    }

    @Override
    public void handleMoveAndRotate(Player user, ByteBufPacketEvent event, int entityId, FriendlyByteBuf buf) {
        this.handleMove(user, event, entityId, buf);
    }

    @Override
    public void handleSyncEntityPosition(Player user, ByteBufPacketEvent event, int entityId, FriendlyByteBuf buf) {
        EquipmentLodTracker tracker = ((BukkitServerPlayer) user).equipmentLod();
        if (tracker != null) tracker.position(entityId, new PacketPosition(buf.readDouble(), buf.readDouble(), buf.readDouble()), 0);
    }

    @Override
    public void handleTeleportEntity(Player user, ByteBufPacketEvent event, int entityId, FriendlyByteBuf buf) {
        EquipmentLodTracker tracker = ((BukkitServerPlayer) user).equipmentLod();
        if (tracker == null) return;
        PacketPosition position = new PacketPosition(buf.readDouble(), buf.readDouble(), buf.readDouble());
        int relatives = 0;
        if (VersionHelper.isOrAbove1_21_2) {
            buf.skipBytes(3 * Double.BYTES + 2 * Float.BYTES);
            relatives = buf.readInt();
        }
        tracker.position(entityId, position, relatives);
    }
}
