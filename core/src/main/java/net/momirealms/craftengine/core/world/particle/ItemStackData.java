package net.momirealms.craftengine.core.world.particle;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.LazyReference;

public class ItemStackData implements ParticleData {
    private final LazyReference<Item<?>> item;

    public ItemStackData(LazyReference<Item<?>> item) {
        this.item = item;
    }

    public Item<?> item() {
        return item.get();
    }
}
