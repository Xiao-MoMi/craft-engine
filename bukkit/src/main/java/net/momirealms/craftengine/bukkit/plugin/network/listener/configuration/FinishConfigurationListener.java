package net.momirealms.craftengine.bukkit.plugin.network.listener.configuration;

import net.momirealms.craftengine.core.plugin.network.ConnectionState;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;

public final class FinishConfigurationListener implements ByteBufferPacketListener {
    public static final ByteBufferPacketListener INSTANCE = new FinishConfigurationListener();

    private FinishConfigurationListener() {}

    @Override
    public void onPacketReceive(NetWorkUser user, ByteBufPacketEvent event) {
        /*
        1.20.2+
        1. receive ServerboundFinishConfigurationPacket
        2. placeNewPlayer
        3. new ServerGamePacketListenerImpl 在 PlayerList
        4. send ClientboundLoginPacket to client

        1.20.5+
        1. receive ServerboundFinishConfigurationPacket
        2. set outbound(encode|s2c) to play
        3. placeNewPlayer
        4. set inbound(decode|c2s) to play
        5. send ClientboundLoginPacket to client
         */
        user.setEncoderState(ConnectionState.PLAY);
    }
}
