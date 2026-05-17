package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import io.netty.buffer.ByteBuf;
import net.momirealms.craftengine.bukkit.plugin.network.BukkitNetworkManager;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.ComponentUtils;
import net.momirealms.craftengine.bukkit.util.PacketUtils;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.context.NetworkTextReplaceContext;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.plugin.text.component.ComponentProvider;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacketProxy;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

public final class PlayerInfoUpdateListener implements ByteBufferPacketListener {
    public static final ByteBufferPacketListener INSTANCE = new PlayerInfoUpdateListener();

    private PlayerInfoUpdateListener() {}

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
        if (!Config.interceptPlayerInfo()) return;
        FriendlyByteBuf buf = event.getBuffer();
        EnumSet<?> actions = buf.readEnumSet((Class) ClientboundPlayerInfoUpdatePacketProxy.ActionProxy.CLASS);
        if (!actions.contains(ClientboundPlayerInfoUpdatePacketProxy.ActionProxy.UPDATE_DISPLAY_NAME)) return;
        List<Object> entries = buf.readList(it -> {
            Object builder = ClientboundPlayerInfoUpdatePacketProxy.EntryBuilderProxy.INSTANCE.newInstance(buf.readUUID());
            ByteBuf nmsFriendlyByteBuf = PacketUtils.ensureNMSFriendlyByteBuf(it);
            for (Object action : actions) {
                Object reader = ClientboundPlayerInfoUpdatePacketProxy.ActionProxy.INSTANCE.getReader(action);
                if (VersionHelper.isOrAbove1_20_5) {
                    ClientboundPlayerInfoUpdatePacketProxy.ActionProxy.ReaderProxy.INSTANCE.read$1(reader, builder, nmsFriendlyByteBuf);
                } else {
                    ClientboundPlayerInfoUpdatePacketProxy.ActionProxy.ReaderProxy.INSTANCE.read$0(reader, builder, nmsFriendlyByteBuf);
                }
            }
            return ClientboundPlayerInfoUpdatePacketProxy.EntryBuilderProxy.INSTANCE.build(builder);
        });
        boolean changed = false;
        for (Object entry : entries) {
            Object mcComponent = ClientboundPlayerInfoUpdatePacketProxy.EntryProxy.INSTANCE.getDisplayName(entry);
            if (mcComponent == null) continue;
            String json = ComponentUtils.minecraftToJson(mcComponent);
            Map<String, ComponentProvider> tokens = BukkitNetworkManager.instance().matchNetworkTags(json);
            if (tokens.isEmpty()) continue;
            ClientboundPlayerInfoUpdatePacketProxy.EntryProxy.INSTANCE.setDisplayName(
                    entry,
                    ComponentUtils.adventureToMinecraft(AdventureHelper.replaceText(AdventureHelper.jsonToComponent(json), tokens, NetworkTextReplaceContext.of((BukkitServerPlayer) user)))
            );
            changed = true;
        }
        if (changed) {
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeEnumSet(actions, (Class) ClientboundPlayerInfoUpdatePacketProxy.ActionProxy.CLASS);
            buf.writeCollection(entries, (it, entry) -> {
                it.writeUUID(ClientboundPlayerInfoUpdatePacketProxy.EntryProxy.INSTANCE.getProfileId(entry));
                ByteBuf nmsFriendlyByteBuf = PacketUtils.ensureNMSFriendlyByteBuf(it);
                for (Object action : actions) {
                    Object writer = ClientboundPlayerInfoUpdatePacketProxy.ActionProxy.INSTANCE.getWriter(action);
                    if (VersionHelper.isOrAbove1_20_5) {
                        ClientboundPlayerInfoUpdatePacketProxy.ActionProxy.WriterProxy.INSTANCE.write$1(writer, nmsFriendlyByteBuf, entry);
                    } else {
                        ClientboundPlayerInfoUpdatePacketProxy.ActionProxy.WriterProxy.INSTANCE.write$0(writer, nmsFriendlyByteBuf, entry);
                    }
                }
            });
        }
    }
}
