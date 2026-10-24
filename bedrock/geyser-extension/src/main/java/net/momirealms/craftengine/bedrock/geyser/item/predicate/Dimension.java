package net.momirealms.craftengine.bedrock.geyser.item.predicate;

import org.geysermc.geyser.api.predicate.MinecraftPredicate;
import org.geysermc.geyser.api.predicate.context.item.ItemPredicateContext;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public record Dimension(List<String> values, boolean negated) implements SessionPredicate {

    @Override
    public boolean test(ItemPredicateContext context) {
        throw new IllegalStateException("Dimension predicate requires a Geyser session");
    }

    @Override
    public MinecraftPredicate<ItemPredicateContext> bind(Supplier<String> dimension, BooleanSupplier selected) {
        // context.dimension() 是维度类型；Java context_dimension 使用实际世界 ID。
        return context -> {
            String world = dimension.get();
            return (world != null && this.values.contains(world)) != this.negated;
        };
    }
}
