package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import net.momirealms.craftengine.core.plugin.network.EntityPacketHandler;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;

public final class MoveEntityPosRotListener implements ByteBufferPacketListener {
    public static final ByteBufferPacketListener INSTANCE = new MoveEntityPosRotListener();

    private MoveEntityPosRotListener() {}

    @Override
    public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
        FriendlyByteBuf buf = event.getBuffer();
        int entityId = buf.readVarInt();
        EntityPacketHandler handler = user.entityPacketHandlers().get(entityId);
        if (handler != null) {
            handler.handleMoveAndRotate(user, event, entityId, buf);
        }
    }
}
