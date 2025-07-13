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

public class SetSaturationFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final PlayerSelector<CTX> selector;
    private final NumberProvider count;
    private final boolean add;

    public SetSaturationFunction(NumberProvider count, boolean add, PlayerSelector<CTX> selector, List<Condition<CTX>> predicates) {
        super(predicates);
        this.count = count;
        this.add = add;
        this.selector = selector;
    }

    @Override
    public void runInternal(CTX ctx) {
        if (this.selector == null) {
            Optional<Player> optionalPlayer = ctx.getOptionalParameter(DirectContextParameters.PLAYER);
            optionalPlayer.ifPresent(player -> player.setSaturation(this.add ? player.saturation() + this.count.getFloat(ctx) : this.count.getFloat(ctx)));
        } else {
            for (Player target : this.selector.get(ctx)) {
                RelationalContext relationalContext = ViewerContext.of(ctx, PlayerOptionalContext.of(target, ContextHolder.EMPTY));
                target.setSaturation(this.add ? target.saturation() + this.count.getFloat(relationalContext) : this.count.getFloat(relationalContext));
            }
        }
    }

    @Override
    public Key type() {
        return CommonFunctions.SET_SATURATION;
    }

    public static class FactoryImpl<CTX extends Context> extends AbstractFactory<CTX> {

        public FactoryImpl(java.util.function.Function<Map<String, Object>, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public Function<CTX> create(Map<String, Object> arguments) {
            Object value = ResourceConfigUtils.requireNonNullOrThrow(arguments.get("saturation"), "warning.config.function.set_saturation.missing_saturation");
            boolean add = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("add", false), "add");
            return new SetSaturationFunction<>(NumberProviders.fromObject(value), add, PlayerSelectors.fromObject(arguments.get("target"), conditionFactory()), getPredicates(arguments));
        }
    }
}
