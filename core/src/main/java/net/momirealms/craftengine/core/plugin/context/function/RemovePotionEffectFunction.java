package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelector;
import net.momirealms.craftengine.core.util.Key;

import java.util.List;

public final class RemovePotionEffectFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final PlayerSelector<CTX> selector;
    private final Key potionEffectType;
    private final boolean all;

    private RemovePotionEffectFunction(List<Condition<CTX>> predicates,
                                       PlayerSelector<CTX> selector,
                                       boolean all,
                                       Key potionEffectType) {
        super(predicates);
        this.potionEffectType = potionEffectType;
        this.selector = selector;
        this.all = all;
    }

    @Override
    public void runInternal(CTX ctx) {
        if (this.selector == null) {
            ctx.getOptionalParameter(DirectContextParameters.PLAYER).ifPresent(it -> {
                if (this.all) it.clearPotionEffects();
                else it.removePotionEffect(this.potionEffectType);
            });
        } else {
            for (Player target : this.selector.get(ctx)) {
                if (this.all) target.clearPotionEffects();
                else target.removePotionEffect(this.potionEffectType);
            }
        }
    }

    public static <CTX extends Context> FunctionFactory<CTX, RemovePotionEffectFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, RemovePotionEffectFunction<CTX>> {
        private static final String[] POTION_EFFECTS = new String[] {"potion_effect", "potion-effect"};

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public RemovePotionEffectFunction<CTX> create(ConfigSection section) {
            if (section.getBoolean("all")) {
                return new RemovePotionEffectFunction<>(
                        getPredicates(section),
                        getPlayerSelector(section), true,
                        null);
            } else {
                return new RemovePotionEffectFunction<>(
                        getPredicates(section),
                        getPlayerSelector(section), false,
                        section.getNonNullIdentifier(POTION_EFFECTS)
                );
            }
        }
    }
}