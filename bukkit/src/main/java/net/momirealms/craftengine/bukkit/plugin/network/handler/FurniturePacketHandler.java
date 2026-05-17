package net.momirealms.craftengine.bukkit.plugin.network.handler;

import it.unimi.dsi.fastutil.ints.IntList;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.FurnitureSnapshotState;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.network.EntityPacketHandler;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;

public final class FurniturePacketHandler implements EntityPacketHandler {
    public final Furniture furniture;
    public final FurnitureSnapshotState snapshotState;

    public FurniturePacketHandler(Furniture furniture) {
        this.furniture = furniture;
        this.snapshotState = furniture.snapshotState();
    }

    @Override
    public boolean handleEntitiesRemove(NetWorkUser user, IntList entityIds) {
        Player player = (Player) user;
        player.removeTrackedEntity(this.furniture.entityId());
        this.snapshotState.hide(player);
        this.furniture.controller.onAsyncPlayerUntrack(player, this.snapshotState);
        return true;
    }

    @Override
    public void handleSyncEntityPosition(NetWorkUser user, ByteBufPacketEvent event, int entityId, FriendlyByteBuf buf) {
        event.setCancelled(true);
    }

    @Override
    public void handleMove(NetWorkUser user, ByteBufPacketEvent event, int entityId, FriendlyByteBuf buf) {
        event.setCancelled(true);
    }
}
