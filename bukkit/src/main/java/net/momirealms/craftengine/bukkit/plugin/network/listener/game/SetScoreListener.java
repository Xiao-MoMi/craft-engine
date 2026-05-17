package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import net.kyori.adventure.text.Component;
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

public final class SetScoreListener implements ByteBufferPacketListener {
    public static final ByteBufferPacketListener INSTANCE = VersionHelper.isOrAbove1_20_3 ? new SetScoreListener() : null;

    private SetScoreListener() {}

    @Override
    public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
        if (!Config.interceptSetScore()) return;
        if (!(user instanceof BukkitServerPlayer serverPlayer)) return;
        boolean isChanged = false;
        FriendlyByteBuf buf = event.getBuffer();
        String owner = buf.readUtf();
        String objectiveName = buf.readUtf();
        int score = buf.readVarInt();
        boolean hasDisplay = buf.readBoolean();
        Tag displayName = null;
        if (hasDisplay) {
            displayName = buf.readNbt(false);
        }
        BukkitNetworkManager networkManager = BukkitNetworkManager.instance();
        outside:
        if (displayName != null) {
            Map<String, ComponentProvider> tokens = networkManager.matchNetworkTags(displayName);
            if (tokens.isEmpty()) break outside;
            Component component = AdventureHelper.tagToComponent(displayName);
            component = AdventureHelper.replaceText(component, tokens, NetworkTextReplaceContext.of(serverPlayer));
            displayName = AdventureHelper.componentToTag(component);
            isChanged = true;
        }
        boolean hasNumberFormat = buf.readBoolean();
        int format = -1;
        Tag style = null;
        Tag fixed = null;
        if (hasNumberFormat) {
            format = buf.readVarInt();
            if (format == 0) {
                if (displayName == null) return;
            } else if (format == 1) {
                if (displayName == null) return;
                style = buf.readNbt(false);
            } else if (format == 2) {
                fixed = buf.readNbt(false);
                if (fixed == null) return;
                Map<String, ComponentProvider> tokens = networkManager.matchNetworkTags(fixed);
                if (tokens.isEmpty() && !isChanged) return;
                if (!tokens.isEmpty()) {
                    Component component = AdventureHelper.tagToComponent(fixed);
                    component = AdventureHelper.replaceText(component, tokens, NetworkTextReplaceContext.of(serverPlayer));
                    fixed = AdventureHelper.componentToTag(component);
                    isChanged = true;
                }
            }
        }
        if (isChanged) {
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeUtf(owner);
            buf.writeUtf(objectiveName);
            buf.writeVarInt(score);
            if (hasDisplay) {
                buf.writeBoolean(true);
                buf.writeNbt(displayName, false);
            } else {
                buf.writeBoolean(false);
            }
            if (hasNumberFormat) {
                buf.writeBoolean(true);
                buf.writeVarInt(format);
                if (format == 1) {
                    buf.writeNbt(style, false);
                } else if (format == 2) {
                    buf.writeNbt(fixed, false);
                }
            } else {
                buf.writeBoolean(false);
            }
        }
    }
}
