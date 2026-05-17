package net.momirealms.craftengine.bukkit.plugin.network.listener.login;

import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.VersionHelper;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

public final class HelloListener implements ByteBufferPacketListener {
    public static final ByteBufferPacketListener INSTANCE = new HelloListener();

    private HelloListener() {}

    @Override
    public void onPacketReceive(NetWorkUser user, ByteBufPacketEvent event) {
        BukkitServerPlayer player = (BukkitServerPlayer) user;
        FriendlyByteBuf buf = event.getBuffer();
        String name = buf.readUtf(16);
        player.setUnverifiedName(name);
        if (VersionHelper.isOrAbove1_20_2) {
            UUID uuid = buf.readUUID();
            player.setUnverifiedUUID(uuid);
        } else {
            Optional<UUID> uuid = buf.readOptional(FriendlyByteBuf::readUUID);
            if (uuid.isPresent()) {
                player.setUnverifiedUUID(uuid.get());
            } else {
                player.setUnverifiedUUID(UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8)));
            }
        }
    }
}
