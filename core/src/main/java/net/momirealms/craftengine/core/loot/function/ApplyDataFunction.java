package net.momirealms.craftengine.core.loot.function;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.processor.ItemProcessor;
import net.momirealms.craftengine.core.item.processor.ItemProcessors;
import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.CommonConditions;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;

import java.util.ArrayList;
import java.util.List;

public final class ApplyDataFunction extends AbstractLootConditionalFunction {
    public static final LootFunctionFactory<ApplyDataFunction> FACTORY = new Factory();
    public final ItemProcessor[] modifiers;

    private ApplyDataFunction(List<Condition<LootContext>> conditions, ItemProcessor[] modifiers) {
        super(conditions);
        this.modifiers = modifiers;
    }

    @Override
    protected Item applyInternal(Item item, LootContext context) {
        ItemBuildContext ctx = ItemBuildContext.of(context.player(), item, context.contexts().copy()
                .withParameter(DirectContextParameters.ITEM, item));
        for (ItemProcessor modifier : this.modifiers) {
            modifier.apply(ctx);
        }
        return ctx.item();
    }

    private static class Factory implements LootFunctionFactory<ApplyDataFunction> {

        @Override
        public ApplyDataFunction create(ConfigSection section) {
            List<ItemProcessor> modifiers = new ArrayList<>();
            ItemProcessors.collectProcessors(section.getNonNullSection("data"), modifiers::add);
            return new ApplyDataFunction(
                    section.getList(ConfigKeys.of("condition(s)"), CommonConditions::fromConfig),
                    modifiers.toArray(new ItemProcessor[0])
            );
        }
    }
}
