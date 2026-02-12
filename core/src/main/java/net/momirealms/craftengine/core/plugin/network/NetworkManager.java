package net.momirealms.craftengine.core.plugin.network;

import io.netty.channel.Channel;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.Manageable;
import net.momirealms.craftengine.core.plugin.text.component.ComponentProvider;
import net.momirealms.craftengine.core.util.StringValueOnlyTagVisitor;
import net.momirealms.sparrow.nbt.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public interface NetworkManager extends Manageable {

    void setUser(Channel channel, NetWorkUser user);

    NetWorkUser getUser(Channel channel);

    NetWorkUser removeUser(Channel channel);

    Channel getChannel(Player player);

    int remapBlockState(int stateId, boolean enableMod);

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

    Map<String, ComponentProvider> matchNetworkTags(String text);

    default Map<String, ComponentProvider> matchNetworkTags(Tag nbt) {
        return matchNetworkTags(new StringValueOnlyTagVisitor().visit(nbt));
    }

    default IllegalCharacterProcessResult processIllegalCharacters(String raw) {
        return processIllegalCharacters(raw, '*');
    }

    IllegalCharacterProcessResult processIllegalCharacters(String raw, char replacement);
}
