package net.momirealms.craftengine.bukkit.plugin.user;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;

public class FakeBukkitServerPlayer extends BukkitServerPlayer {

    public FakeBukkitServerPlayer(BukkitCraftEngine plugin) {
        super(plugin, null);
    }

    @Override
    public Channel nettyChannel() {
        return null;
    }

    @Override
    public ChannelHandler connection() {
        return null;
    }

    @Override
    public void kick(Component message) {
    }

    @Override
    public boolean isFakePlayer() {
        return true;
    }
}
