package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import io.netty.buffer.ByteBuf;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.bukkit.util.PacketUtils;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.trade.MerchantOffer;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.MutableBoolean;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.core.component.DataComponentExactPredicateProxy;
import net.momirealms.craftengine.proxy.minecraft.network.codec.StreamDecoderProxy;
import net.momirealms.craftengine.proxy.minecraft.network.codec.StreamEncoderProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.trading.ItemCostProxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class MerchantOffersListener {
    public static final ByteBufferPacketListener INSTANCE = VersionHelper.isOrAbove1_20_5 ? new V1_20_5() : new V1_20();

    private MerchantOffersListener() {}

    private static class V1_20 implements ByteBufferPacketListener {
        private V1_20() {}

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (Config.disableItemOperations()) return;
            if (!(user instanceof BukkitServerPlayer serverPlayer)) return;
            FriendlyByteBuf buf = event.getBuffer();
            int containerId = buf.readContainerId();
            BukkitItemManager manager = BukkitItemManager.instance();
            List<MerchantOffer> merchantOffers = buf.readCollection(ArrayList::new, byteBuf -> {
                Item cost1 = PacketUtils.readItem(buf);
                Item result = PacketUtils.readItem(buf);
                Item cost2 = PacketUtils.readItem(buf);
                boolean outOfStock = byteBuf.readBoolean();
                int uses = byteBuf.readInt();
                int maxUses = byteBuf.readInt();
                int xp = byteBuf.readInt();
                int specialPrice = byteBuf.readInt();
                float priceMultiplier = byteBuf.readFloat();
                int demand = byteBuf.readInt();
                return new MerchantOffer(cost1, Optional.of(cost2), result, outOfStock, uses, maxUses, xp, specialPrice, priceMultiplier, demand);
            });

            MutableBoolean changed = new MutableBoolean(false);
            for (MerchantOffer offer : merchantOffers) {
                offer.applyClientboundData(item -> {
                    Optional<Item> remapped = manager.s2c(item, serverPlayer);
                    if (remapped.isEmpty()) {
                        return item;
                    }
                    changed.set(true);
                    return remapped.get();
                });
            }

            if (changed.booleanValue()) {
                int villagerLevel = buf.readVarInt();
                int villagerXp = buf.readVarInt();
                boolean showProgress = buf.readBoolean();
                boolean canRestock = buf.readBoolean();

                event.setChanged(true);
                buf.clear();
                buf.writeVarInt(event.packetID());
                buf.writeContainerId(containerId);
                buf.writeCollection(merchantOffers, (byteBuf, offer) -> {
                    PacketUtils.writeItem(buf, offer.cost1());
                    PacketUtils.writeItem(buf, offer.result());
                    PacketUtils.writeItem(buf, offer.cost2().orElseThrow());
                    byteBuf.writeBoolean(offer.outOfStock());
                    byteBuf.writeInt(offer.uses());
                    byteBuf.writeInt(offer.maxUses());
                    byteBuf.writeInt(offer.xp());
                    byteBuf.writeInt(offer.specialPrice());
                    byteBuf.writeFloat(offer.priceMultiplier());
                    byteBuf.writeInt(offer.demand());
                });

                buf.writeVarInt(villagerLevel);
                buf.writeVarInt(villagerXp);
                buf.writeBoolean(showProgress);
                buf.writeBoolean(canRestock);
            }
        }
    }

    private static class V1_20_5 implements ByteBufferPacketListener {
        private V1_20_5() {}

        @SuppressWarnings("unchecked")
        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (Config.disableItemOperations()) return;
            if (!(user instanceof BukkitServerPlayer serverPlayer)) return;
            FriendlyByteBuf buf = event.getBuffer();
            int containerId = buf.readContainerId();
            BukkitItemManager manager = BukkitItemManager.instance();
            ByteBuf friendlyBuf = PacketUtils.ensureNMSFriendlyByteBuf(buf.source());
            List<MerchantOffer> merchantOffers = buf.readCollection(ArrayList::new, byteBuf -> {
                Item cost1 = ItemStackUtils.wrap(ItemCostProxy.INSTANCE.getItemStack(StreamDecoderProxy.INSTANCE.decode(ItemCostProxy.STREAM_CODEC, friendlyBuf)));
                Item result = PacketUtils.readItem(friendlyBuf);
                Optional<Item> cost2 = ((Optional<Object>) StreamDecoderProxy.INSTANCE.decode(ItemCostProxy.OPTIONAL_STREAM_CODEC, friendlyBuf))
                        .map(cost -> ItemStackUtils.wrap(ItemCostProxy.INSTANCE.getItemStack(cost)));
                boolean outOfStock = byteBuf.readBoolean();
                int uses = byteBuf.readInt();
                int maxUses = byteBuf.readInt();
                int xp = byteBuf.readInt();
                int specialPrice = byteBuf.readInt();
                float priceMultiplier = byteBuf.readFloat();
                int demand = byteBuf.readInt();
                return new MerchantOffer(cost1, cost2, result, outOfStock, uses, maxUses, xp, specialPrice, priceMultiplier, demand);
            });

            MutableBoolean changed = new MutableBoolean(false);
            for (MerchantOffer offer : merchantOffers) {
                offer.applyClientboundData(item -> {
                    Optional<Item> remapped = manager.s2c(item, serverPlayer);
                    if (remapped.isEmpty()) {
                        return item;
                    }
                    changed.set(true);
                    return remapped.get();
                });
            }

            if (changed.booleanValue()) {
                int villagerLevel = buf.readVarInt();
                int villagerXp = buf.readVarInt();
                boolean showProgress = buf.readBoolean();
                boolean canRestock = buf.readBoolean();

                event.setChanged(true);
                buf.clear();
                buf.writeVarInt(event.packetID());
                buf.writeContainerId(containerId);
                buf.writeCollection(merchantOffers, (byteBuf, offer) -> {
                    StreamEncoderProxy.INSTANCE.encode(ItemCostProxy.STREAM_CODEC, friendlyBuf, itemStackToItemCost(offer.cost1().minecraftItem(), offer.cost1().count()));
                    PacketUtils.writeItem(friendlyBuf, offer.result());
                    StreamEncoderProxy.INSTANCE.encode(ItemCostProxy.OPTIONAL_STREAM_CODEC, friendlyBuf, offer.cost2().map(it -> itemStackToItemCost(it.minecraftItem(), it.count())));
                    byteBuf.writeBoolean(offer.outOfStock());
                    byteBuf.writeInt(offer.uses());
                    byteBuf.writeInt(offer.maxUses());
                    byteBuf.writeInt(offer.xp());
                    byteBuf.writeInt(offer.specialPrice());
                    byteBuf.writeFloat(offer.priceMultiplier());
                    byteBuf.writeInt(offer.demand());
                });

                buf.writeVarInt(villagerLevel);
                buf.writeVarInt(villagerXp);
                buf.writeBoolean(showProgress);
                buf.writeBoolean(canRestock);
            }
        }
    }

    private static Object itemStackToItemCost(Object itemStack, int count) {
        return ItemCostProxy.INSTANCE.newInstance(
                ItemProxy.INSTANCE.getBuiltInRegistryHolder(ItemStackProxy.INSTANCE.getItem(itemStack)),
                count,
                DataComponentExactPredicateProxy.INSTANCE.allOf(ItemStackProxy.INSTANCE.getComponents(itemStack))
        );
    }
}
