package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.VersionHelper;

import java.util.Optional;

public final class ServerDataListener implements ByteBufferPacketListener {
    public static final ByteBufferPacketListener INSTANCE = VersionHelper.isOrAbove1_20_5 ? null : new ServerDataListener();

    private ServerDataListener() {}

    @Override
    public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
        if (!Config.disableChatReport()) return;
        FriendlyByteBuf buf = event.getBuffer();
        Component motd = buf.readComponent();
        Optional<byte[]> icon = buf.readOptional(FriendlyByteBuf::readByteArray);
        boolean enforcesSecureChat = true; // 去弹窗警告
        event.setChanged(true);
        buf.clear();
        buf.writeVarInt(event.packetID());
        buf.writeComponent(motd);
        buf.writeOptional(icon, FriendlyByteBuf::writeByteArray);
        buf.writeBoolean(enforcesSecureChat);
    }
}
