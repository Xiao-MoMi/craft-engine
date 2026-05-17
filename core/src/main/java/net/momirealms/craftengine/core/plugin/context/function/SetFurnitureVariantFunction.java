package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;

import java.util.List;
import java.util.Optional;

public final class SetFurnitureVariantFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final String variantName;

    private SetFurnitureVariantFunction(List<Condition<CTX>> predicates, String variantName) {
        super(predicates);
        this.variantName = variantName;
    }

    @Override
    public void runInternal(CTX ctx) {
        Optional<Furniture> furnitureOptional = ctx.getOptionalParameter(DirectContextParameters.FURNITURE);
        furnitureOptional.ifPresent(furniture -> furniture.setVariant(variantName));
    }

    public static <CTX extends Context> FunctionFactory<CTX, SetFurnitureVariantFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, SetFurnitureVariantFunction<CTX>> {
        private static final String[] VARIANT = new String[] {"variant", "anchor_type", "anchor-type"};

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public SetFurnitureVariantFunction<CTX> create(ConfigSection section) {

            return new SetFurnitureVariantFunction<>(
                    getPredicates(section),
                    section.getNonNullString(VARIANT)
            );
        }
    }
}