package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;

public final class AddTransientBlockListener implements ByteBufferPacketListener {
    private final int[] blockStateMapper;
    private final int[] modBlockStateMapper;

    public AddTransientBlockListener(int[] blockStateMapper, int[] modBlockStateMapper) {
        this.blockStateMapper = blockStateMapper;
        this.modBlockStateMapper = modBlockStateMapper;
    }

    @Override
    public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
        FriendlyByteBuf buf = event.getBuffer();
        long pos = buf.readLong();
        int before = buf.readVarInt();
        int state = user.clientCustomBlockEnabled() ? this.modBlockStateMapper[before] : this.blockStateMapper[before];
        if (state == before) {
            return;
        }
        event.setChanged(true);
        buf.clear();
        buf.writeVarInt(event.packetID());
        buf.writeLong(pos);
        buf.writeVarInt(state);
    }
}
