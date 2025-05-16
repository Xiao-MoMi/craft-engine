package net.momirealms.craftengine.bukkit.plugin.network.handler;

import net.momirealms.craftengine.core.plugin.network.EntityPacketHandler;
import net.momirealms.craftengine.core.plugin.network.NMSPacketEvent;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;

public class FurnitureCollisionPacketHandler implements EntityPacketHandler {
    public static final FurnitureCollisionPacketHandler INSTANCE = new FurnitureCollisionPacketHandler();

    @Override
    public void handleSyncEntityPosition(NetWorkUser user, NMSPacketEvent event, Object packet) {
        event.setCancelled(true);
    }
}
