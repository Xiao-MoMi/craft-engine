package net.momirealms.craftengine.core.loot.entry;

import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.CommonConditions;
import net.momirealms.craftengine.core.plugin.context.Condition;

import java.util.List;

public final class AlternativesLootEntryContainer extends AbstractCompositeLootEntryContainer {
    public static final LootEntryContainerFactory<AlternativesLootEntryContainer> FACTORY = new Factory();

    private AlternativesLootEntryContainer(List<Condition<LootContext>> conditions, List<LootEntryContainer> children) {
        super(conditions, children);
    }

    @Override
    protected LootEntryContainer compose(List<? extends LootEntryContainer> children) {
        return switch (children.size()) {
            case 0 -> LootEntryContainer.alwaysFalse();
            case 1 -> children.get(0);
            case 2 -> children.get(0).or(children.get(1));
            default -> (context, choiceConsumer) -> {
                for (LootEntryContainer child : children) {
                    if (child.expand(context, choiceConsumer)) {
                        return true;
                    }
                }
                return false;
            };
        };
    }

    private static class Factory implements LootEntryContainerFactory<AlternativesLootEntryContainer> {

        @Override
        public AlternativesLootEntryContainer create(ConfigSection section) {
            return new AlternativesLootEntryContainer(
                    section.getList("conditions", CommonConditions::fromConfig),
                    section.getList("children", LootEntryContainers::fromConfig)
            );
        }
    }
}
