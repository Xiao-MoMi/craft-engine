package net.momirealms.craftengine.bedrock.geyser.item.predicate;

import org.geysermc.geyser.api.predicate.MinecraftPredicate;
import org.geysermc.geyser.api.predicate.context.item.ItemPredicateContext;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public record Selected(boolean expected) implements SelectionPredicate {

    @Override
    public boolean test(ItemPredicateContext context) {
        return !this.expected;
    }

    @Override
    public MinecraftPredicate<ItemPredicateContext> bind(Supplier<String> dimension, BooleanSupplier selected) {
        return context -> selected.getAsBoolean() == this.expected;
    }
}
