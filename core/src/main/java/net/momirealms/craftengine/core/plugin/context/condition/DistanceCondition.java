package net.momirealms.craftengine.core.plugin.context.condition;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.number.ConstantNumberProvider;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.world.WorldPosition;

import java.util.Optional;

// TODO It's designed for players for the moment, better using entities
public final class DistanceCondition<CTX extends Context> implements Condition<CTX> {
    private final NumberProvider min;
    private final NumberProvider max;

    private DistanceCondition(NumberProvider min, NumberProvider max) {
        this.max = max;
        this.min = min;
    }

    @Override
    public boolean test(CTX ctx) {
        float min = this.min.getFloat(ctx);
        float max = this.max.getFloat(ctx);
        Optional<Player> optionalPlayer = ctx.getOptionalParameter(DirectContextParameters.PLAYER);
        if (optionalPlayer.isEmpty()) {
            return false;
        }
        Optional<WorldPosition> optionalPosition = ctx.getOptionalParameter(DirectContextParameters.POSITION);
        if (optionalPosition.isEmpty()) {
            return false;
        }

        WorldPosition location = optionalPosition.get();
        Player player = optionalPlayer.get();
        if (!player.world().uuid().equals(location.world().uuid())) {
            return false;
        }

        double dx = location.x() - player.x();
        double dy = location.y() - player.y();
        double dz = location.z() - player.z();
        double distanceSquared = dx * dx + dy * dy + dz * dz;
        double minSquared = min * min;
        double maxSquared = max * max;

        return distanceSquared >= minSquared && distanceSquared <= maxSquared;
    }

    public static <CTX extends Context> ConditionFactory<CTX, DistanceCondition<CTX>> factory() {
        return new Factory<>();
    }

    private static class Factory<CTX extends Context> implements ConditionFactory<CTX, DistanceCondition<CTX>> {

        @Override
        public DistanceCondition<CTX> create(ConfigSection section) {
            return new DistanceCondition<>(
                    section.getNumber("min", ConfigConstants.CONSTANT_ZERO),
                    section.getNumber("max", ConstantNumberProvider.constant(32))
            );
        }
    }
}