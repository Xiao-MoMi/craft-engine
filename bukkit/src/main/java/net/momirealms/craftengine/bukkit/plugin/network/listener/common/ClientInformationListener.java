package net.momirealms.craftengine.bukkit.plugin.network.listener.common;

import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;

public final class ClientInformationListener implements ByteBufferPacketListener {
    public static final ByteBufferPacketListener INSTANCE = new ClientInformationListener();

    private ClientInformationListener() {}

    @Override
    public void onPacketReceive(NetWorkUser user, ByteBufPacketEvent event) {
        FriendlyByteBuf buf = event.getBuffer();
        String language = buf.readUtf(16);
        ((BukkitServerPlayer) user).setClientLocale(TranslationManager.parseLocale(language));
    }
}
