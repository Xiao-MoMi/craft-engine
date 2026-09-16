package net.momirealms.craftengine.core.plugin.network;

import ca.spottedleaf.concurrentutil.map.concurrent.ints.ConcurrentChainedInt2ObjectHashTable;
import com.mojang.authlib.properties.PropertyMap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.pack.host.ResourcePackDownloadData;
import net.momirealms.craftengine.core.plugin.Plugin;
import net.momirealms.craftengine.core.plugin.network.mod.ClientCustomPacket;
import net.momirealms.craftengine.core.util.IntIdentityList;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.chunk.client.ClientChunk;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.util.List;
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

    void sendCustomPacket(ClientCustomPacket packet);

    void sendCustomPackets(List<? extends ClientCustomPacket> packets);

    void sendByteBufPacket(ByteBuf buf, boolean immediately);

    void kick(@Nullable Component message);

    void simulatePacket(Object packet);

    @ApiStatus.Internal
    ConnectionState decoderState();

    @ApiStatus.Internal
    ConnectionState encoderState();

    World clientSideWorld();

    ConcurrentChainedInt2ObjectHashTable<EntityPacketHandler> entityViews();

    boolean clientCustomBlockEnabled();

    void setClientCustomBlock(boolean enable);

    boolean hasClientMod();

    int clientModProtocol();

    void setClientModProtocol(int version);

    void addResourcePackUUID(UUID uuid);

    boolean isResourcePackLoading(UUID uuid);

    void setShouldProcessFinishConfiguration(boolean shouldProcess);

    boolean shouldProcessFinishConfiguration();

    boolean isChunkTracked(long chunkPos);

    ClientChunk getTrackedChunk(long chunkPos);

    void addTrackedChunk(long chunkPos, ClientChunk chunkStatus);

    void clearTrackedChunks();

    void removeTrackedChunk(long chunkPos);

    IntIdentityList clientBlockList();

    void setClientBlockList(IntIdentityList integers);

    IntIdentityList clientBiomeList();

    void setClientBiomeList(IntIdentityList biomes);

    boolean needsBlockStateBitWidthConversion();

    ProtocolVersion protocolVersion();

    void setProtocolVersion(ProtocolVersion protocolVersion);

    void setConnectionState(ConnectionState connectionState);

    void setDecoderState(ConnectionState decoderState);

    void setEncoderState(ConnectionState encoderState);

    void resendChunks();

    void addResourcePackTasks(List<ResourcePackDownloadData> dataList);

    @Nullable
    InetAddress address();
}
