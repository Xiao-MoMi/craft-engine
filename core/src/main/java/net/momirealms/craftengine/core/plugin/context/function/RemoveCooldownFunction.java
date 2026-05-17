package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.CooldownData;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelector;

import java.util.List;
import java.util.Optional;

public final class RemoveCooldownFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final PlayerSelector<CTX> selector;
    private final String id;
    private final boolean all;

    private RemoveCooldownFunction(List<Condition<CTX>> predicates,
                                   PlayerSelector<CTX> selector,
                                   String id,
                                   boolean all) {
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

    public static <CTX extends Context> FunctionFactory<CTX, RemoveCooldownFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, RemoveCooldownFunction<CTX>> {

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public RemoveCooldownFunction<CTX> create(ConfigSection section) {
            if (section.getBoolean("all")) {
                return new RemoveCooldownFunction<>(
                        getPredicates(section),
                        getPlayerSelector(section),
                        null,
                        true
                );
            } else {
                return new RemoveCooldownFunction<>(
                        getPredicates(section),
                        getPlayerSelector(section),
                        section.getNonNullString("id"),
                        false
                );
            }
        }
    }
}