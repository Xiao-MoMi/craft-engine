package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.PacketUtils;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.plugin.network.protocol.recipe.legacy.LegacyRecipeHolder;
import net.momirealms.craftengine.core.plugin.network.protocol.recipe.modern.SingleInputButtonDisplay;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MutableBoolean;
import net.momirealms.craftengine.core.util.VersionHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class UpdateRecipesListener {
    public static final ByteBufferPacketListener INSTANCE = VersionHelper.isOrAbove1_21_2 ? new V1_21_2() : new V1_20();

    private UpdateRecipesListener() {}

    private static class V1_20 implements ByteBufferPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (Config.disableItemOperations()) return;
            MutableBoolean changed = new MutableBoolean(false);
            FriendlyByteBuf buf = event.getBuffer();
            BukkitItemManager itemManager = BukkitItemManager.instance();
            BukkitServerPlayer player = (BukkitServerPlayer) user;
            List<LegacyRecipeHolder> holders = buf.readCollection(ArrayList::new, byteBuf -> {
                LegacyRecipeHolder holder = LegacyRecipeHolder.read(byteBuf, $ -> PacketUtils.readItem(buf));
                holder.recipe().applyClientboundData(item -> {
                    Optional<Item> remapped = itemManager.s2c(item, player);
                    if (remapped.isEmpty()) {
                        return item;
                    }
                    changed.set(true);
                    return remapped.get();
                });
                return holder;
            });
            if (changed.booleanValue()) {
                event.setChanged(true);
                buf.clear();
                buf.writeVarInt(event.packetID());
                buf.writeCollection(holders, ((byteBuf, recipeHolder)
                        -> recipeHolder.write(byteBuf,
                        ($, item) -> PacketUtils.writeItem(buf, item))));
            }
        }
    }

    private static class V1_21_2 implements ByteBufferPacketListener {
        private V1_21_2() {}

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (Config.disableItemOperations()) return;
            MutableBoolean changed = new MutableBoolean(false);
            FriendlyByteBuf buf = event.getBuffer();
            BukkitItemManager itemManager = BukkitItemManager.instance();
            BukkitServerPlayer player = (BukkitServerPlayer) user;
            Map<Key, List<Integer>> itemSets = buf.readMap(
                    FriendlyByteBuf::readKey,
                    b -> b.readCollection(ArrayList::new, FriendlyByteBuf::readVarInt)
            );
            List<SingleInputButtonDisplay> displays = buf.readCollection(ArrayList::new, b -> {
                SingleInputButtonDisplay display = SingleInputButtonDisplay.read(b, $ -> PacketUtils.readItem(buf));
                display.applyClientboundData(item -> {
                    Optional<Item> remapped = itemManager.s2c(item, player);
                    if (remapped.isEmpty()) {
                        return item;
                    }
                    changed.set(true);
                    return remapped.get();
                });
                return display;
            });
            if (changed.booleanValue()) {
                event.setChanged(true);
                buf.clear();
                buf.writeVarInt(event.packetID());
                buf.writeMap(itemSets,
                        FriendlyByteBuf::writeKey,
                        (b, c) -> b.writeCollection(c, FriendlyByteBuf::writeVarInt)
                );
                buf.writeCollection(displays, (b, d) ->
                        d.write(b, ($, item) -> PacketUtils.writeItem(buf, item)));
            }
        }
    }
}
