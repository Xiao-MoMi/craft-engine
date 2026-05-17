package net.momirealms.craftengine.bukkit.item;

import net.momirealms.craftengine.bukkit.util.EquipmentSlotUtils;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.core.entity.EquipmentSlot;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.ItemWrapper;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.inventory.CraftItemStackProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class BukkitItemWrapper implements ItemWrapper {
    protected final Object itemStack;

    protected BukkitItemWrapper(Object itemStack) {
        this.itemStack = itemStack;
    }

    protected BukkitItemWrapper(final ItemStack itemStack) {
        ItemStack item = ItemStackUtils.ensureCraftItemStack(itemStack);
        this.itemStack = CraftItemStackProxy.INSTANCE.unwrap(item);
    }

    @Override
    public ItemStack platformItem() {
        return ItemStackProxy.INSTANCE.getBukkitStack(minecraftItem());
    }

    @Override
    public Object minecraftItem() {
        return this.itemStack;
    }

    @Override
    public void grow(int amount) {
        ItemStackProxy.INSTANCE.grow(this.itemStack, amount);
    }

    @Override
    public void shrink(int amount) {
        ItemStackProxy.INSTANCE.shrink(this.itemStack, amount);
    }

    @Override
    public void count(int amount) {
        ItemStackProxy.INSTANCE.setCount(this.itemStack, amount);
    }

    @Override
    public int count() {
        return ItemStackProxy.INSTANCE.getCount(this.itemStack);
    }

    @Override
    public void hurtAndBreak(int amount, @NotNull Player player, @Nullable EquipmentSlot slot) {
        ItemStackUtils.hurtAndBreak(this.itemStack, amount, player.serverPlayer(), slot == null ? null : EquipmentSlotUtils.toNMSEquipmentSlot(slot));
    }
}
