package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;

public final class ChatSessionUpdateListener implements ByteBufferPacketListener {
    public static final ByteBufferPacketListener INSTANCE = new ChatSessionUpdateListener();

    private ChatSessionUpdateListener() {}

    @Override
    public void onPacketReceive(NetWorkUser user, ByteBufPacketEvent event) {
        if (Config.disableChatReport()) {
            event.setCancelled(true);
        }
    }
}
