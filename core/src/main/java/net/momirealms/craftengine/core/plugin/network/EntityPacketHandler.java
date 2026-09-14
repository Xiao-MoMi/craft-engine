package net.momirealms.craftengine.core.plugin.network;

import it.unimi.dsi.fastutil.ints.IntList;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;

public interface EntityPacketHandler {

    default void handleAddEntity(Player user, ByteBufPacketEvent event, int entityId, FriendlyByteBuf buf) {
    }

    default boolean handleEntitiesRemove(Player user, int entityId, IntList entityIds) {
        return false;
    }

    default boolean handleSetEquipment(Player user, ByteBufPacketEvent event, int entityId, FriendlyByteBuf buf) {
        return false;
    }

    default void handleTeleportEntity(Player user, ByteBufPacketEvent event, int entityId, FriendlyByteBuf buf) {
    }

    default void handleSetEntityData(Player user, ByteBufPacketEvent event) {
    }

    default void handleSyncEntityPosition(Player user, ByteBufPacketEvent event, int entityId, FriendlyByteBuf buf) {
    }

    default void handleMoveAndRotate(Player user, ByteBufPacketEvent event, int entityId, FriendlyByteBuf buf) {
    }

    default void handleMove(Player user, ByteBufPacketEvent event, int entityId, FriendlyByteBuf buf) {
    }
}
