package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import it.unimi.dsi.fastutil.ints.IntList;
import net.momirealms.craftengine.core.plugin.network.EntityPacketHandler;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;

public final class RemoveEntitiesListener implements ByteBufferPacketListener {
    public static final ByteBufferPacketListener INSTANCE = new RemoveEntitiesListener();

    private RemoveEntitiesListener() {}

    @Override
    public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
        FriendlyByteBuf buf = event.getBuffer();
        boolean changed = false;
        IntList intList = buf.readIntIdList();
        for (int i = 0, size = intList.size(); i < size; i++) {
            int entityId = intList.getInt(i);
            EntityPacketHandler handler = user.entityPacketHandlers().remove(entityId);
            if (handler != null && handler.handleEntitiesRemove(user, intList)) {
                changed = true;
            }
        }
        if (changed) {
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeIntIdList(intList);
        }
    }
}
