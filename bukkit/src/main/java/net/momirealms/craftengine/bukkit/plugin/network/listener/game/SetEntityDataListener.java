package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.core.plugin.network.EntityPacketHandler;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;

public final class SetEntityDataListener implements ByteBufferPacketListener {
    public static final ByteBufferPacketListener INSTANCE = new SetEntityDataListener();

    private SetEntityDataListener() {
    }

    @Override
    public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
        if (!(user instanceof BukkitServerPlayer serverPlayer)) return;
        FriendlyByteBuf buf = event.getBuffer();
        int id = buf.readVarInt();
        EntityPacketHandler handler = user.entityViews().get(id);
        if (handler != null) {
            handler.handleSetEntityData(serverPlayer, event);
        }
    }
}
