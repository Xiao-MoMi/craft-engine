package net.momirealms.craftengine.core.plugin.context.condition;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.GameEdition;

public final class IsBedrockPlayerCondition<CTX extends Context> implements Condition<CTX> {
    public static final IsBedrockPlayerCondition<Context> INSTANCE = new IsBedrockPlayerCondition<>();

    private IsBedrockPlayerCondition() {}

    @Override
    public boolean test(CTX ctx) {
        return ctx.getOptionalParameter(DirectContextParameters.PLAYER)
                .map(Player::gameEdition)
                .map(edition -> edition == GameEdition.BEDROCK)
                .orElse(false);
    }

    public static <CTX extends Context> ConditionFactory<CTX, IsBedrockPlayerCondition<CTX>> factory() {
        return new IsBedrockPlayerCondition.Factory<>();
    }

    private static class Factory<CTX extends Context> implements ConditionFactory<CTX, IsBedrockPlayerCondition<CTX>> {

        @SuppressWarnings("unchecked")
        @Override
        public IsBedrockPlayerCondition<CTX> create(ConfigSection arguments) {
            return (IsBedrockPlayerCondition<CTX>) INSTANCE;
        }
    }
}
