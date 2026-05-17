package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.*;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelector;

import java.util.List;
import java.util.Locale;

public final class LevelerExpFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final PlayerSelector<CTX> selector;
    private final NumberProvider count;
    private final String leveler;
    private final String plugin;

    private LevelerExpFunction(List<Condition<CTX>> predicates,
                               PlayerSelector<CTX> selector,
                               String plugin,
                               NumberProvider count,
                               String leveler) {
        super(predicates);
        this.count = count;
        this.leveler = leveler;
        this.plugin = plugin;
        this.selector = selector;
    }

    @Override
    public void runInternal(CTX ctx) {
        if (this.selector == null) {
            ctx.getOptionalParameter(DirectContextParameters.PLAYER).ifPresent(it -> {
                CraftEngine.instance().compatibilityManager().getLevelerProvider(this.plugin).addExp(it, this.leveler, this.count.getDouble(ctx));
            });
        } else {
            for (Player target : this.selector.get(ctx)) {
                RelationalContext relationalContext = ViewerContext.of(ctx, PlayerOptionalContext.of(target));
                CraftEngine.instance().compatibilityManager().getLevelerProvider(this.plugin).addExp(target, this.leveler, this.count.getDouble(relationalContext));
            }
        }
    }

    public static <CTX extends Context> FunctionFactory<CTX, LevelerExpFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, LevelerExpFunction<CTX>> {
        private static final String[] LEVELER = new String[] {"leveler", "skill", "job"};
        private static final String[] COUNT = new String[] {"count", "exp", "amount"};

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public LevelerExpFunction<CTX> create(ConfigSection section) {
            return new LevelerExpFunction<>(
                    getPredicates(section),
                    getPlayerSelector(section),
                    section.getNonNullString("plugin").toLowerCase(Locale.ROOT),
                    section.getNonNullNumber(COUNT),
                    section.getNonNullString(LEVELER)
            );
        }
    }
}