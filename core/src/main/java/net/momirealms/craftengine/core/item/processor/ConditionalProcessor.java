package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.context.CommonConditions;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.sparrow.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class ConditionalProcessor implements ItemProcessor {
    public static final ItemProcessorFactory<ConditionalProcessor> FACTORY = new Factory();
    private final Predicate<Context> condition;
    private final ItemProcessor[] modifiers;

    public ConditionalProcessor(Predicate<Context> condition, ItemProcessor[] modifiers) {
        this.modifiers = modifiers;
        this.condition = condition;
    }

    @Override
    public Item apply(Item item, ItemBuildContext context) {
        if (this.condition.test(context)) {
            for (ItemProcessor m : this.modifiers) {
                item = item.apply(m, context);
            }
        }
        return item;
    }

    @Override
    public Item prepareNetworkItem(Item item, ItemBuildContext context, CompoundTag networkData) {
        if (this.condition.test(context)) {
            for (ItemProcessor m : this.modifiers) {
                item = m.prepareNetworkItem(item, context, networkData);
            }
        }
        return item;
    }

    private static class Factory implements ItemProcessorFactory<ConditionalProcessor> {

        @Override
        public ConditionalProcessor create(ConfigValue value) {
            ConfigSection section = value.getAsSection();
            List<Condition<Context>> conditions = section.getList("conditions", CommonConditions::fromConfig);
            List<ItemProcessor> modifiers = new ArrayList<>();
            ItemProcessors.collectProcessors(section.getNonNullSection("data"), modifiers::add);
            return new ConditionalProcessor(MiscUtils.allOf(conditions), modifiers.toArray(new ItemProcessor[0]));
        }
    }
}
