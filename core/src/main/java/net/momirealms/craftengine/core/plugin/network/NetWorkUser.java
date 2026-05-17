package net.momirealms.craftengine.core.plugin.network;

import com.mojang.authlib.properties.PropertyMap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.pack.host.ResourcePackDownloadData;
import net.momirealms.craftengine.core.plugin.Plugin;
import net.momirealms.craftengine.core.util.IntIdentityList;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.chunk.client.ClientChunk;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;
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

    boolean isNameVerified();

    void setUnverifiedName(String name);

    void setVerifiedName(String name);

    UUID uuid();

    boolean isUUIDVerified();

    void setUnverifiedUUID(UUID uuid);

    void setVerifiedUUID(UUID uuid);

    PropertyMap propertyMap();

    void setPropertyMap(PropertyMap map);

    void sendPacket(Object packet, boolean immediately);

    void sendPacket(Object packet, boolean immediately, Runnable sendListener);

    void sendPackets(List<Object> packet, boolean immediately);

    void sendPackets(List<Object> packet, boolean immediately, Runnable sendListener);

    void sendCustomPayload(Key channel, byte[] data);

    void kick(@Nullable Component message);

    void simulatePacket(Object packet);

    @ApiStatus.Internal
    ConnectionState decoderState();

    @ApiStatus.Internal
    ConnectionState encoderState();

    World clientSideWorld();

    Object serverPlayer();

    Object platformPlayer();

    Map<Integer, EntityPacketHandler> entityPacketHandlers();

    boolean clientModEnabled();

    void setClientModState(boolean enable);

    void addResourcePackUUID(UUID uuid);

    boolean isResourcePackLoading(UUID uuid);

    void setShouldProcessFinishConfiguration(boolean shouldProcess);

    boolean shouldProcessFinishConfiguration();

    boolean isChunkTracked(long chunkPos);

    ClientChunk getTrackedChunk(long chunkPos);

    void addTrackedChunk(long chunkPos, ClientChunk chunkStatus);

    void clearTrackedChunks();

    void removeTrackedChunk(long chunkPos);

    @Nullable
    IntIdentityList clientBlockList();

    void setClientBlockList(IntIdentityList integers);

    ProtocolVersion protocolVersion();

    void setProtocolVersion(ProtocolVersion protocolVersion);

    void setConnectionState(ConnectionState connectionState);

    void setDecoderState(ConnectionState decoderState);

    void setEncoderState(ConnectionState encoderState);

    void resendChunks();

    /**
     * <code>1.20(.1)</code> 只能在游玩阶段调用</br>
     * <code>1.20.2+</code> 只能在配置阶段调用</br>
     * <code>1.20.3+</code> 可以提供多个资源包，在不支持的版本只会使用列表的首个资源包</br>
     */
    void addResourcePackTasks(List<ResourcePackDownloadData> dataList);
}
