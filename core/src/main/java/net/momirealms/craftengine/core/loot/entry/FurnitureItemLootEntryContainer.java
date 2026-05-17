package net.momirealms.craftengine.core.loot.entry;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.loot.function.LootFunction;
import net.momirealms.craftengine.core.loot.function.LootFunctions;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.CommonConditions;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class FurnitureItemLootEntryContainer extends SingleItemLootEntryContainer {
    public static final LootEntryContainerFactory<FurnitureItemLootEntryContainer> FACTORY = new Factory();
    private final boolean hasFallback;

    private FurnitureItemLootEntryContainer(@Nullable Key item,
                                            List<Condition<LootContext>> conditions,
                                            List<LootFunction> lootFunctions,
                                            int weight,
                                            int quality) {
        super(item, conditions, lootFunctions, weight, quality);
        this.hasFallback = item != null;
    }

    @Override
    protected void createItem(Consumer<Item> lootConsumer, LootContext context) {
        Optional<Item> optionalItem = context.getOptionalParameter(DirectContextParameters.FURNITURE_ITEM);
        if (optionalItem.isPresent()) {
            lootConsumer.accept(optionalItem.get());
        } else if (this.hasFallback) {
            super.createItem(lootConsumer, context);
        }
    }

    private static class Factory implements LootEntryContainerFactory<FurnitureItemLootEntryContainer> {

        @Override
        public FurnitureItemLootEntryContainer create(ConfigSection section) {
            return new FurnitureItemLootEntryContainer(
                    section.getIdentifier("item"),
                    section.getList("conditions", CommonConditions::fromConfig),
                    section.getList("functions", LootFunctions::fromConfig),
                    section.getInt("weight", 1),
                    section.getInt("quality")
            );
        }
    }
}
