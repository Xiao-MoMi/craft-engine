package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.bukkit.Bukkit;
import org.bukkit.World;

public final class RespawnListener implements ByteBufferPacketListener {
    public static final ByteBufferPacketListener INSTANCE = new RespawnListener();

    private RespawnListener() {}

    @Override
    public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
        BukkitServerPlayer player = (BukkitServerPlayer) user;
        player.clearView();
        FriendlyByteBuf buf = event.getBuffer();
        World world;
        if (VersionHelper.isOrAbove1_20_5) {
            /*dimensionType*/ buf.readVarInt();
            Key dimension = buf.readKey();
            world = Bukkit.getWorld(KeyUtils.toNamespacedKey(dimension));
        } else { // 1.20~1.20.4
            /*dimensionType*/ buf.readKey();
            Key dimension = buf.readKey();
            world = Bukkit.getWorld(KeyUtils.toNamespacedKey(dimension));
        }
        if (world != null) {
            player.setClientSideWorld(BukkitAdaptor.adapt(world));
            player.clearTrackedChunks();
            player.furnitureLightData().clearLightData();
            player.clearTrackedBlockEntities();
            player.clearTrackedEntities();
        }
    }
}
