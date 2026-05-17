package net.momirealms.craftengine.core.world;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Direction;

public interface WorldlyContainer extends Container {

    int[] getSlotsForFace(Direction direction);

    boolean canPlaceItemThroughFace(int slot, Item stack, Direction direction);

    boolean canTakeItemThroughFace(int slot, Item stack, Direction direction);
}
