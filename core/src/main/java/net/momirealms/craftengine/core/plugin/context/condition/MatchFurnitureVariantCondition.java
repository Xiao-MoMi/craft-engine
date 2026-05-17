package net.momirealms.craftengine.core.plugin.context.condition;

import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public final class MatchFurnitureVariantCondition<CTX extends Context> implements Condition<CTX> {
    private final Set<String> variants;

    public MatchFurnitureVariantCondition(Collection<String> variants) {
        this.variants = new HashSet<>(variants);
    }

    @Override
    public boolean test(CTX ctx) {
        Optional<Furniture> furniture = ctx.getOptionalParameter(DirectContextParameters.FURNITURE);
        return furniture.filter(value -> this.variants.contains(value.currentVariant().name())).isPresent();
    }

    public static <CTX extends Context> ConditionFactory<CTX, MatchFurnitureVariantCondition<CTX>> factory() {
        return new Factory<>();
    }

    private static class Factory<CTX extends Context> implements ConditionFactory<CTX, MatchFurnitureVariantCondition<CTX>> {
        private static final String[] VARIANTS = new String[] {"variant", "variants"};

        @Override
        public MatchFurnitureVariantCondition<CTX> create(ConfigSection section) {
            return new MatchFurnitureVariantCondition<>(section.getStringList(VARIANTS));
        }
    }
}