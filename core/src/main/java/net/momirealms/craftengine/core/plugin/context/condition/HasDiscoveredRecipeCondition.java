package net.momirealms.craftengine.core.plugin.context.condition;

import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.Key;

public final class HasDiscoveredRecipeCondition<CTX extends Context> implements Condition<CTX> {
    private final Key recipe;

    private HasDiscoveredRecipeCondition(Key recipe) {
        this.recipe = recipe;
    }

    @Override
    public boolean test(CTX ctx) {
        return ctx.getOptionalParameter(DirectContextParameters.PLAYER)
                .map(player -> player.hasDiscoveredRecipe(this.recipe))
                .orElse(false);
    }

    public static <CTX extends Context> ConditionFactory<CTX, HasDiscoveredRecipeCondition<CTX>> factory() {
        return new Factory<>();
    }

    private static class Factory<CTX extends Context> implements ConditionFactory<CTX, HasDiscoveredRecipeCondition<CTX>> {
        private static final String[] RECIPE = ConfigKeys.of("recipe|id");

        @Override
        public HasDiscoveredRecipeCondition<CTX> create(ConfigSection section) {
            return new HasDiscoveredRecipeCondition<>(section.getNonNullIdentifier(RECIPE));
        }
    }
}
