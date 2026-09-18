package net.momirealms.craftengine.core.plugin.network.mod.protocol;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemManager;
import net.momirealms.craftengine.core.item.network.ItemPacketSource;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.network.codec.NetworkCodec;
import net.momirealms.craftengine.core.plugin.network.mod.ClientCustomPacket;
import net.momirealms.craftengine.core.plugin.network.mod.CustomPackets;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record ClientboundCreativeModeTabItemsPacket(Action action, List<Item> itemStacks) implements ClientCustomPacket {
    public static final Key ID = Key.ce("creative_mode_tab_items");
    public static final NetworkCodec<FriendlyByteBuf, ClientboundCreativeModeTabItemsPacket> CODEC = ClientCustomPacket.codec(
            ClientboundCreativeModeTabItemsPacket::encode,
            ClientboundCreativeModeTabItemsPacket::decode
    );

    private static ClientboundCreativeModeTabItemsPacket decode(FriendlyByteBuf buf) {
        Action action = buf.readEnumConstant(Action.class);
        if (action == Action.CLEAR) {
            return new ClientboundCreativeModeTabItemsPacket(Action.CLEAR, List.of());
        } else {
            List<Item> list = buf.readCollection(ArrayList::new, CraftEngine.instance().platform()::readItem);
            return new ClientboundCreativeModeTabItemsPacket(action, list);
        }
    }

    private void encode(FriendlyByteBuf buf) {
        buf.writeEnumConstant(this.action);
        if (this.action == Action.CLEAR) return;
        buf.writeCollection(this.itemStacks, CraftEngine.instance().platform()::writeItem);
    }

    public static List<ClientboundCreativeModeTabItemsPacket> create(@NotNull Player player) {
        if (!CustomPackets.CREATIVE_MODE_TAB_ITEMS.checkPermission(player)) return List.of();
        List<Item> itemStacks = new ArrayList<>();
        ItemManager itemManager = CraftEngine.instance().itemManager();
        List<Key> itemIds = itemManager.orderedItemIds();
        for (Key itemId : itemIds) {
            if (itemManager.isVanillaItem(itemId)) continue;
            Item item = Item.byId(itemId, player);
            if (item == null) continue;
            itemStacks.add(itemManager.s2c(item, player, ItemPacketSource.GENERIC).orElse(item));
        }
        List<ClientboundCreativeModeTabItemsPacket> packets = new ArrayList<>();
        boolean first = true;
        int singletonSize = Config.modChannelCreativeTabMaxItemsPerPacket();
        for (int i = 0; i < itemStacks.size(); i += singletonSize) {
            List<Item> chunk = itemStacks.subList(i, Math.min(i + singletonSize, itemStacks.size()));
            packets.add(new ClientboundCreativeModeTabItemsPacket(first ? Action.SET : Action.ADD, new ArrayList<>(chunk)));
            first = false;
        }
        if (packets.isEmpty()) {
            packets.add(new ClientboundCreativeModeTabItemsPacket(Action.CLEAR, List.of()));
        }
        return packets;
    }

    @Override
    public Key id() {
        return ID;
    }

    @Override
    public NetworkCodec<FriendlyByteBuf, ClientboundCreativeModeTabItemsPacket> codec() {
        return CODEC;
    }

    public enum Action {
        ADD,
        CLEAR,
        SET
    }
}
