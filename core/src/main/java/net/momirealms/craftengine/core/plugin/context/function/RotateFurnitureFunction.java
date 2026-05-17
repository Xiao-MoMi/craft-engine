package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.CommonFunctions;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.world.WorldPosition;

import java.util.List;

public final class RotateFurnitureFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final NumberProvider degree;
    private final List<Function<Context>> successFunctions;
    private final List<Function<Context>> failureFunctions;

    private RotateFurnitureFunction(List<Condition<CTX>> predicates,
                                    NumberProvider degree,
                                    List<Function<Context>> successFunctions,
                                    List<Function<Context>> failureFunctions) {
        super(predicates);
        this.degree = degree;
        this.successFunctions = successFunctions;
        this.failureFunctions = failureFunctions;
    }

    @Override
    public void runInternal(CTX ctx) {
        ctx.getOptionalParameter(DirectContextParameters.FURNITURE).ifPresent(furniture -> rotateFurniture(ctx, furniture));
    }

    public void rotateFurniture(CTX ctx, Furniture furniture) {
        if (!furniture.isValid()) return;
        WorldPosition position = furniture.position();
        WorldPosition newPos = new WorldPosition(position.world, position.x, position.y, position.z, position.xRot, position.yRot + this.degree.getFloat(ctx));
        furniture.moveTo(newPos).thenAccept(success -> {
            if (success) {
                for (Function<Context> successFunction : this.successFunctions) {
                    successFunction.run(ctx);
                }
            } else {
                for (Function<Context> failureFunction : this.failureFunctions) {
                    failureFunction.run(ctx);
                }
            }
        });
    }

    public NumberProvider degree() {
        return this.degree;
    }

    public static <CTX extends Context> FunctionFactory<CTX, RotateFurnitureFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, RotateFurnitureFunction<CTX>> {
        private static final String[] ON_SUCCESS = new String[] {"on_success", "on-success"};
        private static final String[] ON_FAILURE = new String[] {"on_failure", "on-failure"};

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public RotateFurnitureFunction<CTX> create(ConfigSection section) {
            return new RotateFurnitureFunction<>(
                    getPredicates(section),
                    section.getNumber("degree", ConfigConstants.CONSTANT_NINETY),
                    section.getSectionList(ON_SUCCESS, CommonFunctions::fromConfig),
                    section.getSectionList(ON_FAILURE, CommonFunctions::fromConfig)
            );
        }
    }
}