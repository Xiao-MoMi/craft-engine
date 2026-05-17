package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.bukkit.item.BukkitItem;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.bukkit.util.PacketUtils;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.ProtocolVersion;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.ItemUtils;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.core.RegistryProxy;
import net.momirealms.craftengine.proxy.minecraft.core.registries.BuiltInRegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlockProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.RayTraceResult;

public final class SetCreativeModeSlotListener implements ByteBufferPacketListener {
    public static final ByteBufferPacketListener INSTANCE = new SetCreativeModeSlotListener();

    private SetCreativeModeSlotListener() {}

    @Override
    public void onPacketReceive(NetWorkUser user, ByteBufPacketEvent event) {
        if (Config.disableItemOperations()) return;
        if (!(user instanceof BukkitServerPlayer serverPlayer)) return;
        if (!serverPlayer.isCreativeMode()) return;
        FriendlyByteBuf buf = event.getBuffer();
        short slotNum = buf.readShort();
        Item item;
        try {
            item = VersionHelper.isOrAbove1_20_5 ? PacketUtils.readUntrustedItem(buf) : PacketUtils.readItem(buf);
        } catch (Exception e) {
            return;
        }
        if (!user.protocolVersion().isVersionNewerThan(ProtocolVersion.V1_21_4)) {
            BukkitCraftEngine.instance().scheduler().platform().run(() -> {
                handleSetCreativeSlotPacketOnMainThread(serverPlayer, slotNum, item);
            }, null, serverPlayer.platformPlayer());
        }
        BukkitItemManager.instance().c2s(item).ifPresent((newItem) -> {
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeShort(slotNum);
            if (VersionHelper.isOrAbove1_20_5) {
                PacketUtils.writeUntrustedItem(buf, newItem);
            } else {
                PacketUtils.writeItem(buf, newItem);
            }
        });
    }

    private static void handleSetCreativeSlotPacketOnMainThread(BukkitServerPlayer player, int slot, Item item) {
        Player bukkitPlayer = player.platformPlayer();
        if (bukkitPlayer == null) return;
        if (bukkitPlayer.getGameMode() != GameMode.CREATIVE) return;
        if (slot < 36 || slot > 44) return;
        if (ItemUtils.isEmpty(item)) return;
        if (slot - 36 != bukkitPlayer.getInventory().getHeldItemSlot()) {
            return;
        }
        double interactionRange = player.getCachedInteractionRange();
        // do ray trace to get current block
        RayTraceResult result = bukkitPlayer.rayTraceBlocks(interactionRange, FluidCollisionMode.NEVER);
        if (result == null) return;
        Block hitBlock = result.getHitBlock();
        if (hitBlock == null) return;
        ImmutableBlockState state = CraftEngineBlocks.getCustomBlockState(hitBlock);
        // not a custom block
        if (state == null || state.isEmpty()) return;
        Key itemId = state.settings().itemId();
        // no item available
        if (itemId == null) return;
        Object vanillaBlock = BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getBlock(state.visualBlockState().minecraftState());
        Object vanillaBlockItem = BlockProxy.INSTANCE.asItem(vanillaBlock);
        if (vanillaBlockItem == null) return;
        Key addItemId = item.vanillaId();
        Key blockItemId = KeyUtils.identifierToKey(RegistryProxy.INSTANCE.getKey(BuiltInRegistriesProxy.ITEM, vanillaBlockItem));
        if (!addItemId.equals(blockItemId)) return;
        BukkitItem wrappedItem = BukkitItemManager.instance().createWrappedItem(itemId, player);
        if (wrappedItem == null || wrappedItem.isEmpty()) {
            CraftEngine.instance().logger().warn("Item: " + itemId + " is not a valid item");
            return;
        }
        ItemStack itemStack = wrappedItem.getBukkitItem();
        PlayerInventory inventory = bukkitPlayer.getInventory();
        int sameItemSlot = -1;
        int emptySlot = -1;
        for (int i = 0; i < 9 + 27; i++) {
            ItemStack invItem = inventory.getItem(i);
            if (ItemStackUtils.isEmpty(invItem)) {
                if (emptySlot == -1 && i < 9) emptySlot = i;
                continue;
            }
            if (invItem.getType().equals(itemStack.getType()) && invItem.getItemMeta().equals(itemStack.getItemMeta())) {
                if (sameItemSlot == -1) sameItemSlot = i;
            }
        }
        if (sameItemSlot != -1) {
            if (sameItemSlot < 9) {
                inventory.setHeldItemSlot(sameItemSlot);
                ItemStack previousItem = inventory.getItem(slot - 36);
                inventory.setItem(slot - 36, previousItem);
            } else {
                ItemStack sameItem = inventory.getItem(sameItemSlot);
                inventory.setItem(sameItemSlot, new ItemStack(Material.AIR));
                inventory.setItem(slot - 36, sameItem);
            }
        } else {
            if (item.count() == 1) {
                if (ItemStackUtils.isEmpty(inventory.getItem(slot - 36))) {
                    inventory.setItem(slot - 36, itemStack);
                    return;
                }
                if (emptySlot != -1) {
                    inventory.setHeldItemSlot(emptySlot);
                    inventory.setItem(emptySlot, itemStack);
                } else {
                    inventory.setItem(slot - 36, itemStack);
                }
            }
        }
    }
}
