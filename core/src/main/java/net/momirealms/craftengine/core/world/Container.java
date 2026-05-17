package net.momirealms.craftengine.core.world;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

public interface Container extends Iterable<Item> {

    int containerSize();

    boolean isEmpty();

    Item getItem(int slot);

    Item removeItem(int slot, int count);

    Item removeItemNoUpdate(int slot);

    void setItem(int slot, Item item);

    int maxStackSize();

    default int getMaxStackSize(Item item) {
        return Math.min(this.maxStackSize(), item.maxStackSize());
    }

    void setChanged();

    boolean stillValid(Player player);

    default void startOpen(Player player) {
    }

    default void stopOpen(Player player) {
    }

    default boolean canPlaceItem(int slot, Item item) {
        return true;
    }

    // todo 完成对 minecraft container 的封装
    default boolean canTakeItem(Object into, int slot, Item item) {
        return true;
    }

    default boolean hasAnyMatching(Predicate<Item> predicate) {
        for (Item item : this) {
            if (predicate.test(item)) {
                return true;
            }
        }
        return false;
    }

    void clearContent();

    List<Item> contents();

    void setMaxStackSize(int size);

    @Nullable
    WorldPosition position();

    default @NotNull Iterator<Item> iterator() {
        return new ContainerIterator(this);
    }

    class ContainerIterator implements Iterator<Item> {
        private final Container container;
        private int index;
        private final int size;

        public ContainerIterator(Container container) {
            this.container = container;
            this.size = container.containerSize();
        }

        @Override
        public boolean hasNext() {
            return this.index < this.size;
        }

        @Override
        public Item next() {
            if (!this.hasNext()) {
                throw new NoSuchElementException();
            } else {
                return this.container.getItem(this.index++);
            }
        }
    }
}
