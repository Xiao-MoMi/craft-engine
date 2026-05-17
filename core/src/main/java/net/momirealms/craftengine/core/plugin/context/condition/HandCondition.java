package net.momirealms.craftengine.core.plugin.context.condition;

import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;

import java.util.Optional;

public final class HandCondition<CTX extends Context> implements Condition<CTX> {
    private final InteractionHand hand;

    private HandCondition(InteractionHand hand) {
        this.hand = hand;
    }

    @Override
    public boolean test(CTX ctx) {
        Optional<InteractionHand> optional = ctx.getOptionalParameter(DirectContextParameters.HAND);
        if (optional.isPresent()) {
            InteractionHand hand = optional.get();
            return hand.equals(this.hand);
        }
        return false;
    }

    public static <CTX extends Context> ConditionFactory<CTX, HandCondition<CTX>> factory() {
        return new Factory<>();
    }

    private static class Factory<CTX extends Context> implements ConditionFactory<CTX, HandCondition<CTX>> {

        @Override
        public HandCondition<CTX> create(ConfigSection section) {
            return new HandCondition<>(section.getEnum("hand", InteractionHand.class, InteractionHand.MAIN_HAND));
        }
    }
}