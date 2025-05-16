package net.momirealms.craftengine.core.plugin.network;

import it.unimi.dsi.fastutil.ints.IntList;

public interface EntityPacketHandler {

    default boolean handleEntitiesRemove(IntList entityIds) {
        return false;
    }

    default void handleSetEntityData(NetWorkUser user, ByteBufPacketEvent event) {
    }

    default void handleSyncEntityPosition(NetWorkUser user, NMSPacketEvent event, Object packet) {
    }

    default void handleMoveAndRotate(NetWorkUser user, NMSPacketEvent event, Object packet) {
    }
}
