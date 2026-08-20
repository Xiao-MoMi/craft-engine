package net.momirealms.craftengine.core.loot.entry;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.CommonConditions;
import net.momirealms.craftengine.core.plugin.context.Condition;

import java.util.List;
import java.util.function.Consumer;

public final class EmptyLootEntryContainer extends AbstractSingleLootEntryContainer {
    public static final LootEntryContainerFactory<EmptyLootEntryContainer> FACTORY = new Factory();

    private EmptyLootEntryContainer(List<Condition<LootContext>> conditions, int weight, int quality) {
        super(conditions, List.of(), weight, quality);
    }

    @Override
    protected void createItem(Consumer<Item> lootConsumer, LootContext context) {}

    private static class Factory implements LootEntryContainerFactory<EmptyLootEntryContainer> {

        @Override
        public EmptyLootEntryContainer create(ConfigSection section) {
            return new EmptyLootEntryContainer(
                    section.getList(ConfigKeys.of("condition(s)"), CommonConditions::fromConfig),
                    section.getInt("weight", 1),
                    section.getInt("quality")
            );
        }
    }
}
