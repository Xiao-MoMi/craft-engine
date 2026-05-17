package net.momirealms.craftengine.core.loot.entry;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.loot.function.LootFunction;
import net.momirealms.craftengine.core.loot.function.LootFunctions;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.CommonConditions;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.Key;

import java.util.List;
import java.util.function.Consumer;

public class SingleItemLootEntryContainer extends AbstractSingleLootEntryContainer {
    public static final LootEntryContainerFactory<SingleItemLootEntryContainer> FACTORY = new Factory();
    private final Key item;

    protected SingleItemLootEntryContainer(Key item, List<Condition<LootContext>> conditions, List<LootFunction> lootFunctions, int weight, int quality) {
        super(conditions, lootFunctions, weight, quality);
        this.item = item;
    }

    @Override
    protected void createItem(Consumer<Item> lootConsumer, LootContext context) {
        Item tItem = CraftEngine.instance().itemManager().createWrappedItem(this.item, context.getOptionalParameter(DirectContextParameters.PLAYER).orElse(null));
        if (tItem != null) {
            lootConsumer.accept(tItem);
        } else {
            CraftEngine.instance().logger().warn("Failed to drop non-existing loot: " + this.item);
        }
    }

    private static class Factory implements LootEntryContainerFactory<SingleItemLootEntryContainer> {
        private static final String[] ITEM = new String[]{"item", "id"};

        @Override
        public SingleItemLootEntryContainer create(ConfigSection section) {
            return new SingleItemLootEntryContainer(
                    section.getNonNullIdentifier(ITEM),
                    section.getList("conditions", CommonConditions::fromConfig),
                    section.getList("functions", LootFunctions::fromConfig),
                    section.getInt("weight", 1),
                    section.getInt("quality")
            );
        }
    }
}
