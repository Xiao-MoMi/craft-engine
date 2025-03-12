package net.momirealms.craftengine.core.plugin.network;

import io.netty.channel.Channel;
import net.momirealms.craftengine.core.entity.player.Player;

import java.util.Collection;

public interface NetworkManager {
    void setUser(Channel channel, NetWorkUser user);

    NetWorkUser getUser(Channel channel);

    NetWorkUser removeUser(Channel channel);

    Channel getChannel(Player player);

    Collection<? extends NetWorkUser> onlineUsers();

    void init();

    void enable();

    void shutdown();
}
