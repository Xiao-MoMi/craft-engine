package net.momirealms.craftengine.core.item.recipe.remainder;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.number.NumberProviders;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.random.ThreadLocalRandomSource;

public final class FixedCraftRemainder implements CraftRemainder {
    public static final CraftRemainderFactory<FixedCraftRemainder> FACTORY = new Factory();
    private final Key item;
    private final NumberProvider count;

    public FixedCraftRemainder(Key item, NumberProvider count) {
        this.item = item;
        this.count = count;
    }

    @Override
    public Item remainder(Key recipeId, Item item) {
        Item wrappedItem = CraftEngine.instance().itemManager().createWrappedItem(this.item, null);
        if (wrappedItem != null) {
            wrappedItem.count(this.count.getInt(ThreadLocalRandomSource.INSTANCE));
        }
        return wrappedItem;
    }

    private static class Factory implements CraftRemainderFactory<FixedCraftRemainder> {
        private static final String[] COUNT = new String[] {"count", "amount"};

        @Override
        public FixedCraftRemainder create(ConfigSection section) {
            return new FixedCraftRemainder(
                    section.getNonNullIdentifier("item"),
                    section.getValue(COUNT, NumberProviders::fromConfig, ConfigConstants.CONSTANT_ONE)
            );
        }
    }
}
