package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;

public final class SwingListener implements ByteBufferPacketListener {
    public static final ByteBufferPacketListener INSTANCE = new SwingListener();

    private SwingListener() {}

    @Override
    public void onPacketReceive(NetWorkUser user, ByteBufPacketEvent event) {
        BukkitServerPlayer player = (BukkitServerPlayer) user;
        FriendlyByteBuf buf = event.getBuffer();
        int hand = buf.readVarInt();
        if (hand == 0/*main*/) {
            player.onSwingHand();
        }
    }
}
