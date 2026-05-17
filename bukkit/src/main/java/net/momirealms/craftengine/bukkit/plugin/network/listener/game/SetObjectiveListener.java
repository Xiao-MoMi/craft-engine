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

public final class SetObjectiveListener {
    public static final ByteBufferPacketListener INSTANCE = VersionHelper.isOrAbove1_20_3 ? new V1_20_3() : new V1_20();

    private SetObjectiveListener() {}

    private static class V1_20 implements ByteBufferPacketListener {
        private V1_20() {}

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (!Config.interceptScoreboard()) return;
            FriendlyByteBuf buf = event.getBuffer();
            String objective = buf.readUtf();
            byte mode = buf.readByte();
            if (mode != 0 && mode != 2) return;
            String displayName = buf.readUtf();
            int renderType = buf.readVarInt();
            Map<String, ComponentProvider> tokens = BukkitNetworkManager.instance().matchNetworkTags(displayName);
            if (tokens.isEmpty()) return;
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeUtf(objective);
            buf.writeByte(mode);
            buf.writeUtf(AdventureHelper.componentToJson(AdventureHelper.replaceText(AdventureHelper.jsonToComponent(displayName), tokens, NetworkTextReplaceContext.of((BukkitServerPlayer) user))));
            buf.writeVarInt(renderType);
        }
    }

    private static class V1_20_3 implements ByteBufferPacketListener {
        private V1_20_3() {}

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (!Config.interceptScoreboard()) return;
            FriendlyByteBuf buf = event.getBuffer();
            String objective = buf.readUtf();
            byte mode = buf.readByte();
            if (mode != 0 && mode != 2) return;
            Tag displayName = buf.readNbt(false);
            if (displayName == null) return;
            int renderType = buf.readVarInt();
            boolean optionalNumberFormat = buf.readBoolean();
            BukkitNetworkManager networkManager = BukkitNetworkManager.instance();
            if (optionalNumberFormat) {
                int format = buf.readVarInt();
                if (format == 0) {
                    Map<String, ComponentProvider> tokens = networkManager.matchNetworkTags(displayName);
                    if (tokens.isEmpty()) return;
                    event.setChanged(true);
                    buf.clear();
                    buf.writeVarInt(event.packetID());
                    buf.writeUtf(objective);
                    buf.writeByte(mode);
                    buf.writeNbt(AdventureHelper.componentToTag(AdventureHelper.replaceText(AdventureHelper.tagToComponent(displayName), tokens, NetworkTextReplaceContext.of((BukkitServerPlayer) user))), false);
                    buf.writeVarInt(renderType);
                    buf.writeBoolean(true);
                    buf.writeVarInt(0);
                } else if (format == 1) {
                    Map<String, ComponentProvider> tokens = networkManager.matchNetworkTags(displayName);
                    if (tokens.isEmpty()) return;
                    Tag style = buf.readNbt(false);
                    event.setChanged(true);
                    buf.clear();
                    buf.writeVarInt(event.packetID());
                    buf.writeUtf(objective);
                    buf.writeByte(mode);
                    buf.writeNbt(AdventureHelper.componentToTag(AdventureHelper.replaceText(AdventureHelper.tagToComponent(displayName), tokens, NetworkTextReplaceContext.of((BukkitServerPlayer) user))), false);
                    buf.writeVarInt(renderType);
                    buf.writeBoolean(true);
                    buf.writeVarInt(1);
                    buf.writeNbt(style, false);
                } else if (format == 2) {
                    Tag fixed = buf.readNbt(false);
                    if (fixed == null) return;
                    Map<String, ComponentProvider> tokens1 = networkManager.matchNetworkTags(displayName);
                    Map<String, ComponentProvider> tokens2 = networkManager.matchNetworkTags(fixed);
                    if (tokens1.isEmpty() && tokens2.isEmpty()) return;
                    event.setChanged(true);
                    buf.clear();
                    buf.writeVarInt(event.packetID());
                    buf.writeUtf(objective);
                    buf.writeByte(mode);
                    buf.writeNbt(tokens1.isEmpty() ? displayName : AdventureHelper.componentToTag(AdventureHelper.replaceText(AdventureHelper.tagToComponent(displayName), tokens1, NetworkTextReplaceContext.of((BukkitServerPlayer) user))), false);
                    buf.writeVarInt(renderType);
                    buf.writeBoolean(true);
                    buf.writeVarInt(2);
                    buf.writeNbt(tokens2.isEmpty() ? fixed : AdventureHelper.componentToTag(AdventureHelper.replaceText(AdventureHelper.tagToComponent(fixed), tokens2, NetworkTextReplaceContext.of((BukkitServerPlayer) user))), false);
                }
            } else {
                Map<String, ComponentProvider> tokens = networkManager.matchNetworkTags(displayName);
                if (tokens.isEmpty()) return;
                event.setChanged(true);
                buf.clear();
                buf.writeVarInt(event.packetID());
                buf.writeUtf(objective);
                buf.writeByte(mode);
                buf.writeNbt(AdventureHelper.componentToTag(AdventureHelper.replaceText(AdventureHelper.tagToComponent(displayName), tokens, NetworkTextReplaceContext.of((BukkitServerPlayer) user))), false);
                buf.writeVarInt(renderType);
                buf.writeBoolean(false);
            }
        }
    }
}
