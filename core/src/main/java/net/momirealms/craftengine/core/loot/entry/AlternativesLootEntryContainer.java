package net.momirealms.craftengine.core.loot.entry;

import net.momirealms.craftengine.core.loot.condition.LootCondition;
import net.momirealms.craftengine.core.loot.condition.LootConditions;
import net.momirealms.craftengine.core.util.Key;

import java.util.*;

public class AlternativesLootEntryContainer<T> extends AbstractCompositeLootEntryContainer<T> {
    public static final Factory<?> FACTORY = new Factory<>();

    protected AlternativesLootEntryContainer(List<LootCondition> conditions, List<LootEntryContainer<T>> children) {
        super(conditions, children);
    }

    @Override
    protected LootEntryContainer<T> compose(List<? extends LootEntryContainer<T>> children) {
        return switch (children.size()) {
            case 0 -> LootEntryContainer.alwaysFalse();
            case 1 -> children.get(0);
            case 2 -> children.get(0).or(children.get(1));
            default -> (context, choiceConsumer) -> {
                for (LootEntryContainer<T> child : children) {
                    if (child.expand(context, choiceConsumer)) {
                        return true;
                    }
                }
                return false;
            };
        };
    }

    @Override
    public Key type() {
        return LootEntryContainers.ALTERNATIVES;
    }

    public static class Factory<A> implements LootEntryContainerFactory<A> {
        @SuppressWarnings("unchecked")
        @Override
        public LootEntryContainer<A> create(Map<String, Object> arguments) {
            List<LootEntryContainer<A>> containers = Optional.ofNullable(arguments.get("children"))
                    .map(it -> (List<LootEntryContainer<A>>) new ArrayList<LootEntryContainer<A>>(LootEntryContainers.fromMapList((List<Map<String, Object>>) it)))
                    .orElse(Collections.emptyList());
            List<LootCondition> conditions = Optional.ofNullable(arguments.get("conditions"))
                    .map(it -> LootConditions.fromMapList((List<Map<String, Object>>) it))
                    .orElse(Collections.emptyList());
            return new AlternativesLootEntryContainer<>(conditions, containers);
        }
    }
}
