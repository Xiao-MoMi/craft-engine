package net.momirealms.craftengine.core.plugin.network;

import com.google.gson.JsonElement;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.Manageable;
import net.momirealms.craftengine.core.plugin.text.component.ComponentProvider;
import net.momirealms.craftengine.core.util.StringValueOnlyTagVisitor;
import net.momirealms.sparrow.nbt.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public interface NetworkManager extends Manageable {

    void setUser(Channel channel, NetWorkUser user);

    NetWorkUser getUser(Channel channel);

    NetWorkUser removeUser(Channel channel);

    Channel getChannel(Player player);

    @Nullable
    Player getOnlineUser(UUID uuid);

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

    void simulatePacket(@NotNull NetWorkUser player, Object packet);

    boolean hasNetworkTag(String text);

    Map<String, ComponentProvider> matchNetworkTags(String text);

    Map<String, ComponentProvider> matchNetworkTags(JsonElement json);

    Map<String, ComponentProvider> matchNetworkTags(Tag nbt);

    default IllegalCharacterProcessResult processIllegalCharacters(String raw) {
        return processIllegalCharacters(raw, '*');
    }

    IllegalCharacterProcessResult processIllegalCharacters(String raw, char replacement);

    void setServerPortHost(Consumer<ChannelPipeline> pipeline);
}
