package net.momirealms.craftengine.bukkit.world.inventory;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.bukkit.world.BukkitContainer;
import net.momirealms.craftengine.bukkit.world.WorldlyContainerHolder;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.NonNullList;
import net.momirealms.craftengine.core.world.WorldPosition;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BukkitStorageContainer implements BukkitContainer {
    private final NonNullList<Item> items;
    private final List<HumanEntity> viewers;
    private final InventoryHolder owner;
    private int maxStackSize = 99;

    public BukkitStorageContainer(InventoryHolder owner, int size) {
        this.items = NonNullList.withSize(size, Item.empty());
        for (int i = 0; i < size; i++) {
            this.items.set(i, Item.empty());
        }
        this.viewers = new ObjectArrayList<>();
        this.owner = owner;
    }

    @Override
    public void onOpen(HumanEntity player) {
        this.viewers.add(player);
    }

    @Override
    public void onClose(HumanEntity player) {
        this.viewers.remove(player);
    }

    @Override
    public List<HumanEntity> getViewers() {
        return this.viewers;
    }

    @Override
    public @Nullable InventoryHolder getOwner() {
        return this.owner;
    }

    @Override
    public int containerSize() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        for (Item item : this.items) {
            if (!item.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Item getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public Item removeItem(int slot, int count) {
        Item item = this.items.get(slot);
        if (item.isEmpty()) return item;
        Item result;
        if (item.count() <= count) {
            this.setItem(slot, Item.empty());
            result = item;
        } else {
            result = item.copyWithCount(count);
            item.shrink(count);
        }
        this.setChanged();
        return result;
    }

    @Override
    public Item removeItemNoUpdate(int slot) {
        Item item = this.items.get(slot);
        if (item.isEmpty()) return item;
        Item result;
        if (item.count() <= 1) {
            this.setItem(slot, Item.empty());
            result = item;
        } else {
            result = item.copyWithCount(1);
            item.shrink(1);
        }
        return result;
    }

    @Override
    public void setItem(int slot, Item item) {
        this.items.set(slot, item);
        if (!item.isEmpty() && this.maxStackSize() > 0 && item.count() > this.maxStackSize()) {
            item.count(this.maxStackSize());
        }
    }

    @Override
    public int maxStackSize() {
        return this.maxStackSize;
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.owner instanceof WorldlyContainerHolder holder) {
            return player.canInteractPoint(holder.pos().toVec3d(), player.getCachedInteractionRange());
        }
        return true;
    }

    @Override
    public List<Item> contents() {
        return this.items;
    }

    @Override
    public void setMaxStackSize(int size) {
        this.maxStackSize = size;
    }

    @Override
    public @Nullable WorldPosition position() {
        if (this.owner instanceof WorldlyContainerHolder holder) {
            return holder.pos();
        }
        return null;
    }

    @Override
    public void clearContent() {
        this.items.clear();
    }
}
