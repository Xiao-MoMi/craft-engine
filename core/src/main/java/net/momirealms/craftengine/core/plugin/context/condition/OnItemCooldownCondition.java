package net.momirealms.craftengine.core.plugin.context.condition;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.Key;

import java.util.Optional;

public final class OnItemCooldownCondition<CTX extends Context> implements Condition<CTX> {
    private final Key id;

    private OnItemCooldownCondition(Key id) {
        this.id = id;
    }

    @Override
    public boolean test(CTX ctx) {
        Optional<Player> player = ctx.getOptionalParameter(DirectContextParameters.PLAYER);
        return player.isPresent() && player.get().getItemCooldown(this.id) > 0;
    }

    public static <CTX extends Context> ConditionFactory<CTX, OnItemCooldownCondition<CTX>> factory() {
        return new Factory<>();
    }

    private static class Factory<CTX extends Context> implements ConditionFactory<CTX, OnItemCooldownCondition<CTX>> {

        @Override
        public OnItemCooldownCondition<CTX> create(ConfigSection section) {
            return new OnItemCooldownCondition<>(section.getNonNullIdentifier("id"));
        }
    }
}
