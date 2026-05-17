package net.momirealms.craftengine.core.loot.entry;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.CommonConditions;
import net.momirealms.craftengine.core.plugin.context.Condition;

import java.util.List;
import java.util.function.Consumer;

public final class EmptyLoopEntryContainer extends AbstractSingleLootEntryContainer {
    public static final LootEntryContainerFactory<EmptyLoopEntryContainer> FACTORY = new Factory();

    private EmptyLoopEntryContainer(List<Condition<LootContext>> conditions, int weight, int quality) {
        super(conditions, null, weight, quality);
    }

    @Override
    protected void createItem(Consumer<Item> lootConsumer, LootContext context) {}

    private static class Factory implements LootEntryContainerFactory<EmptyLoopEntryContainer> {

        @Override
        public EmptyLoopEntryContainer create(ConfigSection section) {
            return new EmptyLoopEntryContainer(
                    section.getList("conditions", CommonConditions::fromConfig),
                    section.getInt("weight", 1),
                    section.getInt("quality")
            );
        }
    }
}
