package net.momirealms.craftengine.core.loot.entry;

import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.CommonConditions;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;

import java.util.List;
import java.util.function.Consumer;

public final class ExpLootEntryContainer extends AbstractLootEntryContainer {
    public static final LootEntryContainerFactory<ExpLootEntryContainer> FACTORY = new Factory();
    private final NumberProvider value;

    private ExpLootEntryContainer(NumberProvider value, List<Condition<LootContext>> conditions) {
        super(conditions);
        this.value = value;
    }

    @Override
    public boolean expand(LootContext context, Consumer<LootEntry> choiceConsumer) {
        if (super.test(context)) {
            context.getOptionalParameter(DirectContextParameters.POSITION)
                    .ifPresent(it -> it.world().dropExp(it, value.getInt(context)));
            return true;
        } else {
            return false;
        }
    }

    private static class Factory implements LootEntryContainerFactory<ExpLootEntryContainer> {
        private static final String[] COUNT = new String[] {"count", "amount", "exp"};

        @Override
        public ExpLootEntryContainer create(ConfigSection section) {
            return new ExpLootEntryContainer(
                    section.getNonNullNumber(COUNT),
                    section.getList("conditions", CommonConditions::fromConfig)
            );
        }
    }
}
