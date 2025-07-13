package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.CooldownData;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelector;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelectors;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RemoveCooldownFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final PlayerSelector<CTX> selector;
    private final String id;
    private final boolean all;

    public RemoveCooldownFunction(String id, boolean all, PlayerSelector<CTX> selector, List<Condition<CTX>> predicates) {
        super(predicates);
        this.selector = selector;
        this.id = id;
        this.all = all;
    }

    @Override
    public void runInternal(CTX ctx) {
        if (this.selector == null) {
            Optional<Player> optionalPlayer = ctx.getOptionalParameter(DirectContextParameters.PLAYER);
            optionalPlayer.ifPresent(player -> {
                CooldownData data = player.cooldown();
                if (this.all) data.clearCooldowns();
                else data.removeCooldown(this.id);
            });
        } else {
            for (Player target : this.selector.get(ctx)) {
                CooldownData data = target.cooldown();
                if (this.all) data.clearCooldowns();
                else data.removeCooldown(this.id);
            }
        }
    }

    @Override
    public Key type() {
        return CommonFunctions.REMOVE_COOLDOWN;
    }

    public static class FactoryImpl<CTX extends Context> extends AbstractFactory<CTX> {

        public FactoryImpl(java.util.function.Function<Map<String, Object>, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public Function<CTX> create(Map<String, Object> arguments) {
            boolean all = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("all", false), "all");
            if (all) {
                return new RemoveCooldownFunction<>(null, true, PlayerSelectors.fromObject(arguments.get("target"), conditionFactory()), getPredicates(arguments));
            } else {
                String id = ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("id"), "warning.config.function.remove_cooldown.missing_id");
                return new RemoveCooldownFunction<>(id, false, PlayerSelectors.fromObject(arguments.get("target"), conditionFactory()), getPredicates(arguments));
            }
        }
    }
}
