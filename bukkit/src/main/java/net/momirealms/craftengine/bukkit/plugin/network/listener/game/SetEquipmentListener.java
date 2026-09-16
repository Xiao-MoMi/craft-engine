package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import net.momirealms.craftengine.bukkit.plugin.network.handler.EquipmentPacketHandler;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.network.EntityPacketHandler;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;

public final class SetEquipmentListener implements ByteBufferPacketListener {
    public static final SetEquipmentListener INSTANCE = new SetEquipmentListener();

    private SetEquipmentListener() {}

    @Override
    public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
        if (!(user instanceof Player player)) return;
        FriendlyByteBuf buf = event.getBuffer();
        int entityId = buf.readVarInt();
        EntityPacketHandler handler = player.entityViews().get(entityId);
        if (handler == null || !handler.handleSetEquipment(player, event, entityId, buf)) {
            EquipmentPacketHandler.INSTANCE.handleSetEquipment(player, event, entityId, buf);
        }
    }
}
