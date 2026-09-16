package net.momirealms.craftengine.core.entity.furniture.element;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.momirealms.craftengine.core.world.WorldPosition;

import java.util.List;

/** Consumes old elements in order, reserving exact matches before fallback transforms. */
public final class FurnitureElementMatcher {
    public final FurnitureElement[] remaining;
    private Object2IntOpenHashMap<WorldPosition> exactHeads;
    private int[] nextExact;
    private int firstRemaining;

    public FurnitureElementMatcher(List<FurnitureElement> elements) {
        this.remaining = elements.toArray(new FurnitureElement[0]);
    }

    public TransformableFurnitureElement match(Class<? extends TransformableFurnitureElement> elementClass,
                                               WorldPosition position, boolean exact) {
        if (this.firstRemaining == this.remaining.length) return null;
        // Matching config order needs no index. Other exact matches use the position buckets.
        if (exact && this.exactHeads == null) {
            FurnitureElement old = this.remaining[this.firstRemaining];
            if (old instanceof TransformableFurnitureElement transformable
                    && elementClass.isInstance(old) && position.equals(transformable.position())) {
                return consume(this.firstRemaining, transformable);
            }
            buildIndex();
        }
        int first = exact ? this.exactHeads.getInt(position) : this.firstRemaining;
        for (int i = first; i >= 0 && i < this.remaining.length;
             i = exact ? this.nextExact[i] : i + 1) {
            FurnitureElement old = this.remaining[i];
            if (old instanceof TransformableFurnitureElement transformable && elementClass.isInstance(old)) {
                if (exact && i == first) this.exactHeads.put(position, this.nextExact[i]);
                return consume(i, transformable);
            }
        }
        return null;
    }

    private TransformableFurnitureElement consume(int index, TransformableFurnitureElement old) {
        this.remaining[index] = null;
        while (this.firstRemaining < this.remaining.length && this.remaining[this.firstRemaining] == null) {
            this.firstRemaining++;
        }
        return old;
    }

    private void buildIndex() {
        this.exactHeads = new Object2IntOpenHashMap<>(this.remaining.length);
        this.exactHeads.defaultReturnValue(-1);
        this.nextExact = new int[this.remaining.length];
        for (int i = this.remaining.length - 1; i >= this.firstRemaining; i--) {
            FurnitureElement element = this.remaining[i];
            if (element instanceof TransformableFurnitureElement transformable) {
                this.nextExact[i] = this.exactHeads.put(transformable.position(), i);
            }
        }
    }
}
