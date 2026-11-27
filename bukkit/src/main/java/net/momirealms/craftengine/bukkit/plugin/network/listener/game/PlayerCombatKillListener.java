package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import com.google.gson.JsonElement;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.plugin.network.BukkitNetworkManager;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.ComponentUtils;
import net.momirealms.craftengine.bukkit.util.RegistryOps;
import net.momirealms.craftengine.core.item.network.ItemPacketSource;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.context.NetworkTextReplaceContext;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.plugin.text.component.ComponentProvider;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.sparrow.nbt.Tag;

import java.util.Map;

public final class PlayerCombatKillListener {
    public static final ByteBufferPacketListener INSTANCE = VersionHelper.isOrAbove1_20_3 ? new V1_20_3() : new V1_20();

    private PlayerCombatKillListener() {}

    private static class V1_20 implements ByteBufferPacketListener {
        private V1_20() {}

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (!Config.interceptCombatKill() && Config.disableItemOperations()) return;
            FriendlyByteBuf buf = event.getBuffer();
            int playerId = buf.readVarInt();
            String jsonOrPlainString = buf.readUtf();
            Tag tag = RegistryOps.JSON.convertTo(RegistryOps.SPARROW_NBT, GsonHelper.get().fromJson(jsonOrPlainString, JsonElement.class));
            Component component = AdventureHelper.nbtToComponent(tag);
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeVarInt(playerId);
            if (Config.interceptCombatKill()) {
                Map<String, ComponentProvider> tokens = BukkitNetworkManager.instance().matchNetworkTags(jsonOrPlainString);
                if (!tokens.isEmpty()) {
                    component = AdventureHelper.replaceText(component, tokens, NetworkTextReplaceContext.of((BukkitServerPlayer) user));
                }
            }
            if (!Config.disableItemOperations()) {
                component = AdventureHelper.replaceShowItem(component, s -> ComponentUtils.replaceShowItem(s, (BukkitServerPlayer) user, ItemPacketSource.MESSAGE));
            }
            buf.writeUtf(RegistryOps.SPARROW_NBT.convertTo(RegistryOps.JSON, AdventureHelper.componentToNbt(component)).toString());
        }
    }

    private static class V1_20_3 implements ByteBufferPacketListener {
        private V1_20_3() {}

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (!Config.interceptCombatKill() && Config.disableItemOperations()) return;
            FriendlyByteBuf buf = event.getBuffer();
            int playerId = buf.readVarInt();
            Tag nbt = buf.readNbt(false);
            if (nbt == null) return;
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeVarInt(playerId);
            Component component = AdventureHelper.tagToComponent(nbt);
            if (Config.interceptCombatKill()) {
                Map<String, ComponentProvider> tokens = BukkitNetworkManager.instance().matchNetworkTags(nbt);
                if (!tokens.isEmpty()) {
                    component = AdventureHelper.replaceText(component, tokens, NetworkTextReplaceContext.of((BukkitServerPlayer) user));
                }
            }
            if (!Config.disableItemOperations()) {
                component = AdventureHelper.replaceShowItem(component, s -> ComponentUtils.replaceShowItem(s, (BukkitServerPlayer) user, ItemPacketSource.MESSAGE));
            }
            buf.writeNbt(AdventureHelper.componentToTag(component), false);
        }
    }
}
