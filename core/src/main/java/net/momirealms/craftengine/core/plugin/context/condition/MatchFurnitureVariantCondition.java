package net.momirealms.craftengine.core.plugin.context.condition;

import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.util.*;

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

        @Override
        public MatchFurnitureVariantCondition<CTX> create(Map<String, Object> arguments) {
            List<String> variants = MiscUtils.getAsStringList(ResourceConfigUtils.get(arguments, "variant", "variants"));
            return new MatchFurnitureVariantCondition<>(variants);
        }
    }
}