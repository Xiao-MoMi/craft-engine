package net.momirealms.craftengine.bedrock.geyser.item.predicate;

import org.geysermc.geyser.api.predicate.MinecraftPredicate;
import org.geysermc.geyser.api.predicate.context.item.ItemPredicateContext;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public interface SessionPredicate extends MinecraftPredicate<ItemPredicateContext> {

    MinecraftPredicate<ItemPredicateContext> bind(Supplier<String> dimension, BooleanSupplier selected);
}
