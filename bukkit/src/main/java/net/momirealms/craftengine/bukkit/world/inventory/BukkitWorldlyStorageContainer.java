package net.momirealms.craftengine.bukkit.world.inventory;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.world.WorldlyContainer;
import org.bukkit.inventory.InventoryHolder;

import java.util.stream.IntStream;

public class BukkitWorldlyStorageContainer extends BukkitStorageContainer implements WorldlyContainer {
    private final int[] slots;
    private final boolean canPlaceItem;
    private final boolean canTakeItem;

    public BukkitWorldlyStorageContainer(InventoryHolder owner, int size, boolean canPlaceItem, boolean canTakeItem) {
        super(owner, size);
        this.slots = IntStream.range(0, size).toArray();
        this.canPlaceItem = canPlaceItem;
        this.canTakeItem = canTakeItem;
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        return this.slots;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, Item stack, Direction direction) {
        return this.canPlaceItem;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, Item stack, Direction direction) {
        return this.canTakeItem;
    }
}
