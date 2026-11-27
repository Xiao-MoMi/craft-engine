package net.momirealms.craftengine.core.block.entity.tick;

import it.unimi.dsi.fastutil.objects.ReferenceArrayList;

import java.util.Arrays;
import java.util.BitSet;

import static net.momirealms.craftengine.core.block.entity.tick.ReplaceableTickingBlockEntity.INDEX_PENDING_ADDITION;
import static net.momirealms.craftengine.core.block.entity.tick.ReplaceableTickingBlockEntity.INDEX_UNREGISTERED;

public final class ActiveBlockEntityTickersList {
    private final Entries entries = new Entries();
    private final ReferenceArrayList<ReplaceableTickingBlockEntity> pendingAdditions = new ReferenceArrayList<>();
    private final BitSet pendingSleep = new BitSet();
    private final BitSet pendingRemovals = new BitSet();
    private int activeCount;
    private boolean ticking;

    private static void updateIndices(Object[] elements, int first, int count) {
        for (int i = first; i < first + count; i++) {
            ((ReplaceableTickingBlockEntity) elements[i]).index(i);
        }
    }

    public int activeSize() {
        return this.activeCount;
    }

    public int size() {
        return this.entries.size();
    }

    private void add(ReplaceableTickingBlockEntity ticker) {
        if (this.ticking) {
            ticker.index(INDEX_PENDING_ADDITION); // State changes are read when it is appended.
            this.pendingAdditions.add(ticker);
        } else {
            int index = this.entries.size();
            this.entries.add(ticker);
            ticker.index(index);
            if (!ticker.isSleeping()) swap(index, this.activeCount++);
        }
    }

    void stateChanged(ReplaceableTickingBlockEntity ticker) {
        int index = ticker.index();
        if (index == INDEX_PENDING_ADDITION) return;
        if (!ticker.isValid()) {
            if (index >= 0) {
                this.pendingSleep.clear(index);
                this.pendingRemovals.set(index);
            } else {
                ticker.detachTickerListener();
            }
            return;
        }
        if (index == INDEX_UNREGISTERED) {
            add(ticker);
            return;
        }
        this.pendingRemovals.clear(index); // A rebind may revive a wrapper before batch removal.
        if (ticker.isSleeping()) {
            if (index >= this.activeCount) return;
            if (this.ticking) this.pendingSleep.set(index);
            else swap(index, --this.activeCount);
        } else {
            this.pendingSleep.clear(index);
            if (index >= this.activeCount) swap(index, this.activeCount++);
        }
    }

    private void swap(int first, int second) {
        if (first == second) return;
        ReplaceableTickingBlockEntity a = this.entries.get(first);
        ReplaceableTickingBlockEntity b = this.entries.get(second);
        this.entries.set(first, b);
        this.entries.set(second, a);
        if (this.pendingRemovals.get(first) != this.pendingRemovals.get(second)) {
            this.pendingRemovals.flip(first);
            this.pendingRemovals.flip(second);
        }
        a.index(second);
        b.index(first);
    }

    public void tick() {
        removeMarkedEntries();
        this.ticking = true;
        try {
            for (int i = 0; i < this.activeCount; i++) {
                ReplaceableTickingBlockEntity ticker = this.entries.get(i);
                if (ticker.isValid()) ticker.tick();
                else this.pendingRemovals.set(i);
            }
        } finally {
            this.ticking = false;
            // Descending indices preserve unprocessed sleep marks, as in Leaf.
            for (int index = this.pendingSleep.length() - 1; index >= 0; index = this.pendingSleep.previousSetBit(index - 1)) {
                swap(index, --this.activeCount);
            }
            this.pendingSleep.clear();
            removeMarkedEntries();
            for (ReplaceableTickingBlockEntity ticker : this.pendingAdditions) {
                ticker.index(INDEX_UNREGISTERED);
                stateChanged(ticker);
            }
            this.pendingAdditions.clear();
        }
    }

    private void removeMarkedEntries() {
        int removed = this.pendingRemovals.length() - 1;
        if (removed < 0) return;
        Object[] elements = this.entries.elements();
        int oldSize = this.entries.size();
        int newSize = oldSize;
        int newActiveCount = this.activeCount;
        while (removed >= 0) {
            int first = this.pendingRemovals.previousClearBit(removed) + 1;
            int end = removed + 1;
            int count = end - first;
            for (int i = first; i < end; i++) {
                ReplaceableTickingBlockEntity ticker = (ReplaceableTickingBlockEntity) elements[i];
                ticker.index(INDEX_UNREGISTERED);
                ticker.detachTickerListener();
            }
            if (first < newActiveCount) {
                int oldActiveCount = newActiveCount;
                newActiveCount -= Math.min(end, oldActiveCount) - first;
                int moved = Math.min(count, newActiveCount - first);
                if (moved > 0) {
                    System.arraycopy(elements, oldActiveCount - moved, elements, first, moved);
                    updateIndices(elements, first, moved);
                }
            }
            int hole = Math.max(first, newActiveCount);
            int oldEnd = newSize;
            newSize -= count;
            int moved = Math.min(count, newSize - hole);
            if (moved > 0) {
                System.arraycopy(elements, oldEnd - moved, elements, hole, moved);
                updateIndices(elements, hole, moved);
            }
            removed = this.pendingRemovals.previousSetBit(first - 1);
        }
        Arrays.fill(elements, newSize, oldSize, null);
        this.entries.truncate(newSize);
        this.activeCount = newActiveCount;
        this.pendingRemovals.clear();
    }

    private static final class Entries extends ReferenceArrayList<ReplaceableTickingBlockEntity> {

        void truncate(int size) {
            this.size = size;
        }
    }
}
