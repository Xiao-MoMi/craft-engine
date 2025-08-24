package net.momirealms.craftengine.core.plugin.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.plugin.Plugin;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

public interface NetWorkUser {
    boolean isOnline();

    // 对假人来说会null
    @Nullable
    Channel nettyChannel();

    // 对假人来说会null
    @Nullable
    ChannelHandler connection();

    boolean isFakePlayer();

    Plugin plugin();

    String name();

    void setName(String name);

    UUID uuid();

    void setUUID(UUID uuid);

    void sendPacket(Object packet, boolean immediately);

    void sendPacket(Object packet, boolean immediately, Runnable sendListener);

    void sendCustomPayload(Key channel, byte[] data);

    void kick(Component message);

    void simulatePacket(Object packet);

    @ApiStatus.Internal
    ConnectionState decoderState();

    @ApiStatus.Internal
    ConnectionState encoderState();

    int clientSideSectionCount();

    Key clientSideDimension();

    Object serverPlayer();

    Object platformPlayer();

    Map<Integer, EntityPacketHandler> entityPacketHandlers();

    boolean clientModEnabled();

    void setClientModState(boolean enable);

    void addResourcePackUUID(UUID uuid);

    boolean isResourcePackLoading(UUID uuid);

    void setShouldProcessFinishConfiguration(boolean shouldProcess);

    boolean shouldProcessFinishConfiguration();
}
