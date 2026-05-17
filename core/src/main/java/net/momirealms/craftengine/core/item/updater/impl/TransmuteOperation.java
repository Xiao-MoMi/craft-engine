package net.momirealms.craftengine.core.item.updater.impl;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.updater.ItemUpdater;
import net.momirealms.craftengine.core.item.updater.ItemUpdaterFactory;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Key;

public final class TransmuteOperation implements ItemUpdater {
    public static final ItemUpdaterFactory<TransmuteOperation> FACTORY = new Factory();
    private final Key newMaterial;

    public TransmuteOperation(Key newMaterial) {
        this.newMaterial = newMaterial;
    }

    @Override
    public Item update(Item item, ItemBuildContext context) {
        return item.transmuteCopy(this.newMaterial, item.count());
    }

    private static class Factory implements ItemUpdaterFactory<TransmuteOperation> {

        @Override
        public TransmuteOperation create(Key item, ConfigSection section) {
            return new TransmuteOperation(section.getNonNullIdentifier("material"));
        }
    }
}
