package net.momirealms.craftengine.core.item.recipe.transform;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.processor.ItemProcessor;
import net.momirealms.craftengine.core.item.processor.ItemProcessors;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;

import java.util.ArrayList;
import java.util.List;

public final class ApplyData implements ItemTransformDataProcessor {
    public static final ItemTransformDataProcessor.Factory<ApplyData> FACTORY = new Factory();
    private final ItemProcessor[] modifiers;

    public ApplyData(ItemProcessor[] modifiers) {
        this.modifiers = modifiers;
    }

    @Override
    public void accept(Item item1, Item item2, Item item3) {
        for (ItemProcessor modifier : this.modifiers) {
            item3.apply(modifier, ItemBuildContext.EMPTY);
        }
    }

    private static class Factory implements ItemTransformDataProcessor.Factory<ApplyData> {

        @Override
        public ApplyData create(ConfigSection section) {
            List<ItemProcessor> modifiers = new ArrayList<>();
            ItemProcessors.collectProcessors(section.getNonNullSection("data"), modifiers::add);
            return new ApplyData(modifiers.toArray(new ItemProcessor[0]));
        }
    }
}