package net.momirealms.craftengine.core.plugin.context.condition;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;

import java.util.Optional;

public final class OnCooldownCondition<CTX extends Context> implements Condition<CTX> {
    private final String key;

    private OnCooldownCondition(String key) {
        this.key = key;
    }

    @Override
    public boolean test(CTX ctx) {
        Optional<Player> player = ctx.getOptionalParameter(DirectContextParameters.PLAYER);
        if (player.isPresent()) {
            Player p = player.get();
            return p.cooldown().isOnCooldown(this.key);
        }
        return false;
    }

    public static <CTX extends Context> ConditionFactory<CTX, OnCooldownCondition<CTX>> factory() {
        return new Factory<>();
    }

    private static class Factory<CTX extends Context> implements ConditionFactory<CTX, OnCooldownCondition<CTX>> {

        @Override
        public OnCooldownCondition<CTX> create(ConfigSection section) {
            return new OnCooldownCondition<>(section.getNonNullString("id"));
        }
    }
}