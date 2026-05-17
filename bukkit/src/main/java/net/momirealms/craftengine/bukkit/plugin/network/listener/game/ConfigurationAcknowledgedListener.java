package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import net.momirealms.craftengine.core.plugin.network.ConnectionState;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;

public final class ConfigurationAcknowledgedListener implements ByteBufferPacketListener {
    public static final ByteBufferPacketListener INSTANCE = new ConfigurationAcknowledgedListener();

    private ConfigurationAcknowledgedListener() {}

    @Override
    public void onPacketReceive(NetWorkUser user, ByteBufPacketEvent event) {
        /*
        1.20.2+
        1. receive ServerboundConfigurationAcknowledgedPacket
        2. setListener ServerConfigurationPacketListenerImpl

        1.20.5+
        1. receive ServerboundConfigurationAcknowledgedPacket
        2. set inbound(decode|c2s) to configuration
         */
        user.setDecoderState(ConnectionState.CONFIGURATION); // in
    }
}
