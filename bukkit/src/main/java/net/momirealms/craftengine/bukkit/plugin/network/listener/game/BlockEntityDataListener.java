package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.bukkit.util.RegistryOps;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Pair;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.proxy.minecraft.nbt.CompoundTagProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.ListTag;
import net.momirealms.sparrow.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class BlockEntityDataListener implements ByteBufferPacketListener {
    public static final ByteBufferPacketListener INSTANCE = new BlockEntityDataListener();

    private BlockEntityDataListener() {}

    @Override
    public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
        if (!Config.interceptItem()) return;
        FriendlyByteBuf buf = event.getBuffer();
        boolean changed = false;
        BlockPos pos = buf.readBlockPos();
        int entityType = buf.readVarInt();
        boolean named = !VersionHelper.isOrAbove1_20_2;
        CompoundTag tag = (CompoundTag) buf.readNbt(named);
        // todo 刷怪笼里的物品？

        // 通用方块实体存储的物品
        if (tag != null && tag.containsKey("Items")) {
            BukkitItemManager itemManager = BukkitItemManager.instance();
            ListTag itemsTag = tag.getList("Items");
            List<Pair<Byte, Item>> items = new ArrayList<>();
            for (Tag itemTag : itemsTag) {
                if (itemTag instanceof CompoundTag itemCompoundTag) {
                    byte slot = itemCompoundTag.getByte("Slot");
                    Object nmsStack;
                    if (VersionHelper.isOrAbove1_20_5) {
                        nmsStack = ItemStackProxy.INSTANCE.getCodec().parse(RegistryOps.SPARROW_NBT, itemCompoundTag)
                                .resultOrPartial((error) -> CraftEngine.instance().logger().error("Tried to parse invalid item: '" + error + "'")).orElse(null);
                    } else {
                        Object nmsTag = RegistryOps.SPARROW_NBT.convertTo(RegistryOps.NBT, itemTag);
                        nmsStack = ItemStackProxy.INSTANCE.of(nmsTag);
                    }
                    Item item = ItemStackUtils.wrap(nmsStack);
                    Optional<Item> optional = itemManager.s2c(item, (BukkitServerPlayer) user);
                    if (optional.isPresent()) {
                        changed = true;
                        items.add(new Pair<>(slot, optional.get()));
                    } else {
                        items.add(Pair.of(slot, item));
                    }
                }
            }
            if (changed) {
                ListTag newItemsTag = new ListTag();
                for (Pair<Byte, Item> pair : items) {
                    CompoundTag newItemCompoundTag;
                    if (VersionHelper.isOrAbove1_20_5) {
                        newItemCompoundTag = (CompoundTag) ItemStackProxy.INSTANCE.getCodec().encodeStart(RegistryOps.SPARROW_NBT, pair.right().minecraftItem())
                                .resultOrPartial((error) -> CraftEngine.instance().logger().error("Tried to encode invalid item: '" + error + "'")).orElse(null);
                    } else {
                        Object nmsTag = ItemStackProxy.INSTANCE.save(pair.right().minecraftItem(), CompoundTagProxy.INSTANCE.newInstance());
                        newItemCompoundTag = (CompoundTag) RegistryOps.NBT.convertTo(RegistryOps.SPARROW_NBT, nmsTag);
                    }
                    if (newItemCompoundTag != null) {
                        newItemCompoundTag.putByte("Slot", pair.left());
                        newItemsTag.add(newItemCompoundTag);
                    }
                }
                tag.put("Items", newItemsTag);
            }
        }
        if (changed) {
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeBlockPos(pos);
            buf.writeVarInt(entityType);
            buf.writeNbt(tag, named);
        }
    }
}
