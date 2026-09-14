package net.momirealms.craftengine.bukkit.plugin.network.handler;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.network.EntityPacketHandler;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;

public final class FurnitureCollisionPacketHandler implements EntityPacketHandler {
    public static final FurnitureCollisionPacketHandler INSTANCE = new FurnitureCollisionPacketHandler();

    @Override
    public void handleSyncEntityPosition(Player user, ByteBufPacketEvent event, int entityId, FriendlyByteBuf buf) {
        event.setCancelled(true);
    }

    @Override
    public void handleMove(Player user, ByteBufPacketEvent event, int entityId, FriendlyByteBuf buf) {
        event.setCancelled(true);
    }
}