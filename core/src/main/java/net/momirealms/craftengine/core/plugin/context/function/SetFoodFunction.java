package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.context.*;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.number.NumberProviders;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelector;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelectors;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SetFoodFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final PlayerSelector<CTX> selector;
    private final NumberProvider count;
    private final boolean add;

    public SetFoodFunction(NumberProvider count, boolean add, PlayerSelector<CTX> selector, List<Condition<CTX>> predicates) {
        super(predicates);
        this.count = count;
        this.add = add;
        this.selector = selector;
    }

    @Override
    public void runInternal(CTX ctx) {
        if (this.selector == null) {
            Optional<Player> optionalPlayer = ctx.getOptionalParameter(DirectContextParameters.PLAYER);
            optionalPlayer.ifPresent(player -> player.setFoodLevel(this.add ? player.foodLevel() + this.count.getInt(ctx) : this.count.getInt(ctx)));
        } else {
            for (Player target : this.selector.get(ctx)) {
                RelationalContext relationalContext = ViewerContext.of(ctx, PlayerOptionalContext.of(target, ContextHolder.EMPTY));
                target.setFoodLevel(this.add ? target.foodLevel() + this.count.getInt(relationalContext) : this.count.getInt(relationalContext));
            }
        }
    }

    @Override
    public Key type() {
        return CommonFunctions.SET_FOOD;
    }

    public static class FactoryImpl<CTX extends Context> extends AbstractFactory<CTX> {

        public FactoryImpl(java.util.function.Function<Map<String, Object>, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public Function<CTX> create(Map<String, Object> arguments) {
            Object value = ResourceConfigUtils.requireNonNullOrThrow(arguments.get("food"), "warning.config.function.set_food.missing_food");
            boolean add = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("add", false), "add");
            return new SetFoodFunction<>(NumberProviders.fromObject(value), add, PlayerSelectors.fromObject(arguments.get("target"), conditionFactory()), getPredicates(arguments));
        }
    }
}
