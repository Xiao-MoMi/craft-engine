package net.momirealms.craftengine.bukkit.plugin.network.listener.login;

import net.momirealms.craftengine.core.plugin.network.ConnectionState;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;

public final class LoginAcknowledgedListener implements ByteBufferPacketListener {
    public static final ByteBufferPacketListener INSTANCE = new LoginAcknowledgedListener();

    private LoginAcknowledgedListener() {}

    @Override
    public void onPacketReceive(NetWorkUser user, ByteBufPacketEvent event) {
        /*
        1.20.2+
        1. receive ServerboundLoginAcknowledgedPacket
        2. new ServerConfigurationPacketListenerImpl 然后直接 startConfiguration
        3. send ClientboundCustomPayloadPacket(BrandPayload) to client

        1.20.5+
        1. receive ServerboundLoginAcknowledgedPacket
        2. set outbound(encode|s2c) to configuration
        3. set inbound(decode|c2s) to configuration
        4. startConfiguration
        5. send ClientboundCustomPayloadPacket(BrandPayload) to client
         */
        user.setConnectionState(ConnectionState.CONFIGURATION);
    }
}
