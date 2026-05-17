package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import net.momirealms.craftengine.bukkit.plugin.network.BukkitNetworkManager;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.context.NetworkTextReplaceContext;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.plugin.text.component.ComponentProvider;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.sparrow.nbt.Tag;

import java.util.Map;

public final class SetActionBarTextListener {
    public static final ByteBufferPacketListener INSTANCE = VersionHelper.isOrAbove1_20_3 ? new V1_20_3() : new V1_20();

    private SetActionBarTextListener() {}

    private static class V1_20 implements ByteBufferPacketListener {
        private V1_20() {}

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (!Config.interceptActionBar()) return;
            FriendlyByteBuf buf = event.getBuffer();
            String json = buf.readUtf();
            Map<String, ComponentProvider> tokens = BukkitNetworkManager.instance().matchNetworkTags(json);
            if (tokens.isEmpty()) return;
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeUtf(AdventureHelper.componentToJson(AdventureHelper.replaceText(AdventureHelper.jsonToComponent(json), tokens, NetworkTextReplaceContext.of((BukkitServerPlayer) user))));
        }
    }

    private static class V1_20_3 implements ByteBufferPacketListener {
        private V1_20_3() {}

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (!Config.interceptActionBar()) return;
            FriendlyByteBuf buf = event.getBuffer();
            Tag nbt = buf.readNbt(false);
            if (nbt == null) return;
            Map<String, ComponentProvider> tokens = BukkitNetworkManager.instance().matchNetworkTags(nbt);
            if (tokens.isEmpty()) return;
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeNbt(AdventureHelper.componentToTag(AdventureHelper.replaceText(AdventureHelper.tagToComponent(nbt), tokens, NetworkTextReplaceContext.of((BukkitServerPlayer) user))), false);
        }
    }
}
