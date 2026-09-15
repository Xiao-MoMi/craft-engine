package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import com.google.gson.JsonElement;
import com.mojang.authlib.GameProfile;
import net.momirealms.craftengine.bukkit.plugin.network.BukkitNetworkManager;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.ComponentUtils;
import net.momirealms.craftengine.bukkit.util.PacketUtils;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.context.NetworkTextReplaceContext;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.NMSPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.NMSPacketListener;
import net.momirealms.craftengine.core.plugin.text.component.ComponentProvider;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacketProxy;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class NMSPlayerInfoUpdateListener implements NMSPacketListener {
    public static final NMSPlayerInfoUpdateListener INSTANCE = new NMSPlayerInfoUpdateListener();

    private NMSPlayerInfoUpdateListener() {}

    @Override
    public void onPacketSend(NetWorkUser user, NMSPacketEvent event, Object packet) {
        if (!(user instanceof BukkitServerPlayer serverPlayer)) return;
        if (!Config.interceptPlayerInfo()) return;
        EnumSet<? extends Enum<?>> actions = ClientboundPlayerInfoUpdatePacketProxy.INSTANCE.getActions(packet);
        if (!actions.contains(ClientboundPlayerInfoUpdatePacketProxy.ActionProxy.UPDATE_DISPLAY_NAME)) return;
        List<Object> entries = ClientboundPlayerInfoUpdatePacketProxy.INSTANCE.getEntries(packet);
        List<Object> newEntries = null;
        for (int i = 0, size = entries.size(); i < size; i++) {
            Object newEntry = replaceDisplayName(serverPlayer, entries.get(i));
            if (newEntry == null) continue;
            if (newEntries == null) newEntries = new ArrayList<>(entries);
            newEntries.set(i, newEntry);
        }
        if (newEntries != null) {
            PacketUtils.replacePacket(event, packet, ClientboundPlayerInfoUpdatePacketProxy.INSTANCE.newInstance(actions, newEntries));
        }
    }

    private static Object replaceDisplayName(BukkitServerPlayer user, Object entry) {
        ClientboundPlayerInfoUpdatePacketProxy.EntryProxy proxy = ClientboundPlayerInfoUpdatePacketProxy.EntryProxy.INSTANCE;
        Object displayName = proxy.getDisplayName(entry);
        if (displayName == null || !ComponentUtils.hasNetworkTag(displayName)) return null;
        JsonElement json = ComponentUtils.minecraftToJsonElement(displayName);
        Map<String, ComponentProvider> tokens = BukkitNetworkManager.instance().matchNetworkTags(json);
        if (tokens.isEmpty()) return null;
        Object newDisplayName = ComponentUtils.adventureToMinecraft(AdventureHelper.replaceText(AdventureHelper.jsonElementToComponent(json), tokens, NetworkTextReplaceContext.of(user)));
        // 一个包会发给多个玩家，必须构建新的 Entry 而不是原地修改
        UUID profileId = proxy.getProfileId(entry);
        GameProfile profile = proxy.getProfile(entry);
        boolean listed = proxy.isListed(entry);
        int latency = proxy.getLatency(entry);
        Object gameMode = proxy.getGameMode(entry);
        Object chatSession = proxy.getChatSession(entry);
        if (VersionHelper.isOrAbove1_21_4) {
            return proxy.newInstance(profileId, profile, listed, latency, gameMode, newDisplayName, proxy.isShowHat(entry), proxy.getListOrder(entry), chatSession);
        }
        if (VersionHelper.isOrAbove1_21_2) {
            return proxy.newInstance(profileId, profile, listed, latency, gameMode, newDisplayName, proxy.getListOrder(entry), chatSession);
        }
        return proxy.newInstance(profileId, profile, listed, latency, gameMode, newDisplayName, chatSession);
    }
}
