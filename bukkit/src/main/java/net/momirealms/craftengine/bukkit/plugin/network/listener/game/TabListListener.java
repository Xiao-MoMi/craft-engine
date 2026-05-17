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

public final class TabListListener {
    public static final ByteBufferPacketListener INSTANCE = VersionHelper.isOrAbove1_20_3 ? new V1_20_3() : new V1_20();

    private TabListListener() {}

    private static class V1_20 implements ByteBufferPacketListener {
        private V1_20() {}

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (!Config.interceptTabList()) return;
            FriendlyByteBuf buf = event.getBuffer();
            String json1 = buf.readUtf();
            String json2 = buf.readUtf();
            BukkitNetworkManager networkManager = BukkitNetworkManager.instance();
            Map<String, ComponentProvider> tokens1 = networkManager.matchNetworkTags(json1);
            Map<String, ComponentProvider> tokens2 = networkManager.matchNetworkTags(json2);
            if (tokens1.isEmpty() && tokens2.isEmpty()) return;
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            NetworkTextReplaceContext context = NetworkTextReplaceContext.of((BukkitServerPlayer) user);
            buf.writeUtf(tokens1.isEmpty() ? json1 : AdventureHelper.componentToJson(AdventureHelper.replaceText(AdventureHelper.jsonToComponent(json1), tokens1, context)));
            buf.writeUtf(tokens2.isEmpty() ? json2 : AdventureHelper.componentToJson(AdventureHelper.replaceText(AdventureHelper.jsonToComponent(json2), tokens2, context)));
        }
    }

    private static class V1_20_3 implements ByteBufferPacketListener {
        private V1_20_3() {}

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (!Config.interceptTabList()) return;
            FriendlyByteBuf buf = event.getBuffer();
            Tag nbt1 = buf.readNbt(false);
            if (nbt1 == null) return;
            Tag nbt2 = buf.readNbt(false);
            if (nbt2 == null) return;
            BukkitNetworkManager networkManager = BukkitNetworkManager.instance();
            Map<String, ComponentProvider> tokens1 = networkManager.matchNetworkTags(nbt1);
            Map<String, ComponentProvider> tokens2 = networkManager.matchNetworkTags(nbt2);
            if (tokens1.isEmpty() && tokens2.isEmpty()) return;
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            NetworkTextReplaceContext context = NetworkTextReplaceContext.of((BukkitServerPlayer) user);
            buf.writeNbt(tokens1.isEmpty() ? nbt1 : AdventureHelper.componentToTag(AdventureHelper.replaceText(AdventureHelper.tagToComponent(nbt1), tokens1, context)), false);
            buf.writeNbt(tokens2.isEmpty() ? nbt2 : AdventureHelper.componentToTag(AdventureHelper.replaceText(AdventureHelper.tagToComponent(nbt2), tokens2, context)), false);
        }
    }
}
