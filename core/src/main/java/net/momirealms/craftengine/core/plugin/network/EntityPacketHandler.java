package net.momirealms.craftengine.core.plugin.network;

import it.unimi.dsi.fastutil.ints.IntList;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;

public interface EntityPacketHandler {

    default boolean handleEntitiesRemove(NetWorkUser user, IntList entityIds) {
        return false;
    }

    default void handleSetEntityData(Player user, ByteBufPacketEvent event) {
    }

    default void handleSyncEntityPosition(NetWorkUser user, ByteBufPacketEvent event, int entityId, FriendlyByteBuf buf) {
    }

    default void handleMoveAndRotate(NetWorkUser user, ByteBufPacketEvent event, int entityId, FriendlyByteBuf buf) {
    }

    default void handleMove(NetWorkUser user, ByteBufPacketEvent event, int entityId, FriendlyByteBuf buf) {
    }
}
