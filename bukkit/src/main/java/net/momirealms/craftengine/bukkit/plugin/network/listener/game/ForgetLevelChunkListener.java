package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.world.BukkitWorldManager;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.ChunkPos;
import net.momirealms.craftengine.core.world.chunk.CEChunk;

public final class ForgetLevelChunkListener implements ByteBufferPacketListener {
    public static final ByteBufferPacketListener INSTANCE = new ForgetLevelChunkListener();

    private ForgetLevelChunkListener() {}

    @Override
    public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
        BukkitServerPlayer player = (BukkitServerPlayer) user;
        FriendlyByteBuf buf = event.getBuffer();
        CEWorld ceWorld = BukkitWorldManager.instance().getWorld(player.world().uuid());
        if (VersionHelper.isOrAbove1_20_2) {
            long chunkPos = buf.readLong();
            user.removeTrackedChunk(chunkPos);
            CEChunk ceChunk = ceWorld.getChunkAtIfLoaded(chunkPos);
            if (ceChunk != null) {
                ceChunk.despawnBlockEntities(player);
            }
        } else {
            int x = buf.readInt();
            int y = buf.readInt();
            user.removeTrackedChunk(ChunkPos.asLong(x, y));
            CEChunk ceChunk = ceWorld.getChunkAtIfLoaded(x, y);
            if (ceChunk != null) {
                ceChunk.despawnBlockEntities(player);
            }
        }
    }
}
