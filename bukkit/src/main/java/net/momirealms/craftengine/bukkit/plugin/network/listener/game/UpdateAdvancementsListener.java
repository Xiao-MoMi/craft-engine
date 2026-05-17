package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import com.google.common.collect.Sets;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.plugin.network.BukkitNetworkManager;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.PacketUtils;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.context.NetworkTextReplaceContext;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.plugin.network.protocol.advancement.AdvancementHolder;
import net.momirealms.craftengine.core.plugin.network.protocol.advancement.AdvancementProgress;
import net.momirealms.craftengine.core.plugin.text.component.ComponentProvider;
import net.momirealms.craftengine.core.util.*;

import java.util.*;

public final class UpdateAdvancementsListener implements ByteBufferPacketListener {
    public static final ByteBufferPacketListener INSTANCE = new UpdateAdvancementsListener();

    private UpdateAdvancementsListener() {}

    @Override
    public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
        if (Config.disableItemOperations() && !Config.interceptAdvancement()) return;
        MutableBoolean changed = new MutableBoolean(false);
        FriendlyByteBuf buf = event.getBuffer();
        BukkitItemManager itemManager = BukkitItemManager.instance();
        BukkitServerPlayer player = (BukkitServerPlayer) user;
        boolean reset = buf.readBoolean();
        List<AdvancementHolder> added = buf.readCollection(ArrayList::new, byteBuf -> {
            AdvancementHolder holder = AdvancementHolder.read(byteBuf, $ -> PacketUtils.readItem(buf));
            if (!Config.disableItemOperations()) {
                holder.applyClientboundData(item -> {
                    Optional<Item> remapped = itemManager.s2c(item, player);
                    if (remapped.isEmpty()) {
                        return item;
                    }
                    changed.set(true);
                    return remapped.get();
                });
            }
            if (Config.interceptAdvancement()) {
                holder.replaceNetworkTags(component -> {
                    Map<String, ComponentProvider> tokens = BukkitNetworkManager.instance().matchNetworkTags(AdventureHelper.componentToJson(component));
                    if (tokens.isEmpty()) return component;
                    changed.set(true);
                    return AdventureHelper.replaceText(component, tokens, NetworkTextReplaceContext.of(player));
                });
            }
            return holder;
        });

        if (changed.booleanValue()) {
            Set<Key> removed = buf.readCollection(Sets::newLinkedHashSetWithExpectedSize, FriendlyByteBuf::readKey);
            Map<Key, AdvancementProgress> progress = buf.readMap(FriendlyByteBuf::readKey, AdvancementProgress::read);

            boolean showAdvancement = false;
            if (VersionHelper.isOrAbove1_21_5) {
                showAdvancement = buf.readBoolean();
            }

            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());

            buf.writeBoolean(reset);
            buf.writeCollection(added, (byteBuf, advancementHolder) -> advancementHolder.write(byteBuf,
                    ($, item) -> PacketUtils.writeItem(buf, item)));
            buf.writeCollection(removed, FriendlyByteBuf::writeKey);
            buf.writeMap(progress, FriendlyByteBuf::writeKey, (byteBuf, advancementProgress) -> advancementProgress.write(byteBuf));
            if (VersionHelper.isOrAbove1_21_5) {
                buf.writeBoolean(showAdvancement);
            }
        }
    }
}
