package net.momirealms.craftengine.core.plugin.network;

import io.netty.channel.Channel;
import net.momirealms.craftengine.core.plugin.Plugin;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.ApiStatus;

public interface NetWorkUser {
    boolean isOnline();

    Channel nettyChannel();

    Plugin plugin();

    String name();

    void sendPacket(Object packet, boolean immediately);

    /**
     * This is not a stable api for developers to use
     *
     * @return connection state
     */
    @ApiStatus.Internal
    ConnectionState decoderState();

    /**
     * This is not a stable api for developers to use
     *
     * @return connection state
     */
    @ApiStatus.Internal
    ConnectionState encoderState();

    int clientSideSectionCount();

    Key clientSideDimension();

    Object serverPlayer();

    Object platformPlayer();
}
