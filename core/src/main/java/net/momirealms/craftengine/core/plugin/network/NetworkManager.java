package net.momirealms.craftengine.core.plugin.network;

import io.netty.channel.Channel;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.Manageable;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface NetworkManager extends Manageable {
    String MOD_CHANNEL = "craftengine:payload";
    String VIA_CHANNEL = "vv:proxy_details";
    Key MOD_CHANNEL_KEY = Key.of(MOD_CHANNEL);
    Key VIA_CHANNEL_KEY = Key.of(VIA_CHANNEL);

    void setUser(Channel channel, NetWorkUser user);

    NetWorkUser getUser(Channel channel);

    NetWorkUser removeUser(Channel channel);

    Channel getChannel(Player player);

    Player[] onlineUsers();

    default void sendPacket(@NotNull NetWorkUser player, Object packet) {
        sendPacket(player, packet, false, null);
    }

    default void sendPacket(@NotNull NetWorkUser player, Object packet, boolean immediately) {
        sendPacket(player, packet, immediately, null);
    }

    void sendPacket(@NotNull NetWorkUser player, Object packet, boolean immediately, Runnable sendListener);

    default void sendPackets(@NotNull NetWorkUser player, List<Object> packet) {
        sendPackets(player, packet, false, null);
    }

    default void sendPackets(@NotNull NetWorkUser player, List<Object> packet, boolean immediately) {
        sendPackets(player, packet, immediately, null);
    }

    void sendPackets(@NotNull NetWorkUser player, List<Object> packet, boolean immediately, Runnable sendListener);
}
