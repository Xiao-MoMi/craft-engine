package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelector;
import net.momirealms.craftengine.core.util.Key;

import java.util.List;

public final class DiscoverRecipeFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final PlayerSelector<CTX> selector;
    private final Key recipe;

    private DiscoverRecipeFunction(List<Condition<CTX>> predicates, PlayerSelector<CTX> selector, Key recipe) {
        super(predicates);
        this.selector = selector;
        this.recipe = recipe;
    }

    @Override
    protected void runInternal(CTX ctx) {
        if (this.selector == null) {
            ctx.getOptionalParameter(DirectContextParameters.PLAYER).ifPresent(player -> player.discoverRecipe(this.recipe));
        } else {
            for (Player player : this.selector.get(ctx)) {
                player.discoverRecipe(this.recipe);
            }
        }
    }

    public static <CTX extends Context> FunctionFactory<CTX, DiscoverRecipeFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, DiscoverRecipeFunction<CTX>> {
        private static final String[] RECIPE = ConfigKeys.of("recipe|id");

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public DiscoverRecipeFunction<CTX> create(ConfigSection section) {
            return new DiscoverRecipeFunction<>(getPredicates(section), getPlayerSelector(section), section.getNonNullIdentifier(RECIPE));
        }
    }
}
