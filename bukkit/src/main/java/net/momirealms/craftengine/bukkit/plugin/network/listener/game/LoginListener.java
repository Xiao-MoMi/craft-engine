package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import com.google.common.collect.Sets;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.network.ConnectionState;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.GlobalPos;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.Optional;
import java.util.Set;

public final class LoginListener implements ByteBufferPacketListener {
    public static final ByteBufferPacketListener INSTANCE = new LoginListener();

    private LoginListener() {}

    @Override
    public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
        BukkitServerPlayer player = (BukkitServerPlayer) user;
        FriendlyByteBuf buf = event.getBuffer();
        if (VersionHelper.isOrAbove1_20_2) {
            {
                /*
                网络切换相关
                1.20.2+
                1. send ClientboundLoginPacket to client

                1.20.5+
                1. set inbound(decode|c2s) to play
                2. send ClientboundLoginPacket to client
                 */
                user.setDecoderState(ConnectionState.PLAY);
            }
            int playerId = buf.readInt();
            boolean hardcore = buf.readBoolean();
            Set<Key> levels = buf.readCollection(Sets::newHashSetWithExpectedSize, FriendlyByteBuf::readKey);
            int maxPlayers = buf.readVarInt();
            int chunkRadius = buf.readVarInt();
            int simulationDistance = buf.readVarInt();
            boolean reducedDebugInfo = buf.readBoolean();
            boolean showDeathScreen = buf.readBoolean();
            boolean doLimitedCrafting = buf.readBoolean();
            if (VersionHelper.isOrAbove1_20_5) {
                int dimensionType = buf.readVarInt();
                Key dimension = buf.readKey();
                World world = Bukkit.getWorld(KeyUtils.toNamespacedKey(dimension));
                if (world != null) {
                    player.setClientSideWorld(BukkitAdaptor.adapt(world));
                }
                if (Config.disableChatReport()) {
                    // 去除弹窗警告
                    long seed = buf.readLong();
                    byte gameType = buf.readByte();
                    byte previousGameType = buf.readByte();
                    boolean isDebug = buf.readBoolean();
                    boolean isFlat = buf.readBoolean();
                    Optional<GlobalPos> lastDeathLocation = buf.readOptional(FriendlyByteBuf::readGlobalPos);
                    int portalCooldown = buf.readVarInt();
                    int seaLevel = VersionHelper.isOrAbove1_21_2 ? buf.readVarInt() : 0;
                    boolean enforcesSecureChat = true;
                    event.setChanged(true);
                    buf.clear();
                    buf.writeVarInt(event.packetID());
                    buf.writeInt(playerId);
                    buf.writeBoolean(hardcore);
                    buf.writeCollection(levels, FriendlyByteBuf::writeKey);
                    buf.writeVarInt(maxPlayers);
                    buf.writeVarInt(chunkRadius);
                    buf.writeVarInt(simulationDistance);
                    buf.writeBoolean(reducedDebugInfo);
                    buf.writeBoolean(showDeathScreen);
                    buf.writeBoolean(doLimitedCrafting);
                    buf.writeVarInt(dimensionType);
                    buf.writeKey(dimension);
                    buf.writeLong(seed);
                    buf.writeByte(gameType);
                    buf.writeByte(previousGameType);
                    buf.writeBoolean(isDebug);
                    buf.writeBoolean(isFlat);
                    buf.writeOptional(lastDeathLocation, FriendlyByteBuf::writeGlobalPos);
                    buf.writeVarInt(portalCooldown);
                    if (VersionHelper.isOrAbove1_21_2) buf.writeVarInt(seaLevel);
                    buf.writeBoolean(enforcesSecureChat);
                }
            } else { // 1.20.2~1.20.4
                /*dimensionType*/ buf.readKey();
                Key dimension = buf.readKey();
                World world = Bukkit.getWorld(KeyUtils.toNamespacedKey(dimension));
                if (world != null) {
                    player.setClientSideWorld(BukkitAdaptor.adapt(world));
                }
            }
        } else { // 1.20(.1)
            /*playerId*/ buf.readInt();
            /*hardcore*/ buf.readBoolean();
            /*gameType*/ buf.readByte();
            /*previousGameType*/ buf.readByte();
            /*levels*/ buf.readCollection(Sets::newHashSetWithExpectedSize, FriendlyByteBuf::readKey);
            /*registryHolder*/ buf.readNbt(true);
            /*dimensionType*/ buf.readKey();
            Key dimension = buf.readKey();
            World world = Bukkit.getWorld(KeyUtils.toNamespacedKey(dimension));
            if (world != null) {
                player.setClientSideWorld(BukkitAdaptor.adapt(world));
            }
        }
    }
}
