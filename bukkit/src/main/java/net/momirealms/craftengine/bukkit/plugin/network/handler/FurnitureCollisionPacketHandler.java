package net.momirealms.craftengine.bukkit.plugin.network.handler;

import net.momirealms.craftengine.core.plugin.network.EntityPacketHandler;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;

public final class FurnitureCollisionPacketHandler implements EntityPacketHandler {
    public static final FurnitureCollisionPacketHandler INSTANCE = new FurnitureCollisionPacketHandler();

    @Override
    public void handleSyncEntityPosition(NetWorkUser user, ByteBufPacketEvent event, int entityId, FriendlyByteBuf buf) {
        event.setCancelled(true);
    }

    @Override
    public void handleMove(NetWorkUser user, ByteBufPacketEvent event, int entityId, FriendlyByteBuf buf) {
        event.setCancelled(true);
    }
}