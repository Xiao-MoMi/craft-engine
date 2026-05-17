package net.momirealms.craftengine.core.item.recipe.result;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.processor.ItemProcessor;
import net.momirealms.craftengine.core.item.processor.ItemProcessors;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;

import java.util.ArrayList;
import java.util.List;

public final class ApplyItemDataPostProcessor implements PostProcessor {
    public static final PostProcessorFactory<ApplyItemDataPostProcessor> FACTORY = new Factory();
    private final ItemProcessor[] modifiers;

    public ApplyItemDataPostProcessor(ItemProcessor[] modifiers) {
        this.modifiers = modifiers;
    }

    @Override
    public Item process(Item item, ItemBuildContext context) {
        for (ItemProcessor modifier : this.modifiers) {
            item.apply(modifier, context);
        }
        return item;
    }

    private static class Factory implements PostProcessorFactory<ApplyItemDataPostProcessor> {

        @Override
        public ApplyItemDataPostProcessor create(ConfigSection section) {
            List<ItemProcessor> modifiers = new ArrayList<>();
            ItemProcessors.collectProcessors(section.getNonNullSection("data"), modifiers::add);
            return new ApplyItemDataPostProcessor(modifiers.toArray(new ItemProcessor[0]));
        }
    }
}