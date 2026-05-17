package net.momirealms.craftengine.core.plugin.context.condition;

import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.world.ExistingBlock;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldPosition;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public final class MatchBlockCondition<CTX extends Context> implements Condition<CTX> {
    private final Set<String> ids;
    private final boolean regexMatch;
    private final NumberProvider x;
    private final NumberProvider y;
    private final NumberProvider z;

    private MatchBlockCondition(Collection<String> ids, boolean regexMatch, NumberProvider x, NumberProvider y, NumberProvider z) {
        this.ids = new HashSet<>(ids);
        this.regexMatch = regexMatch;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public boolean test(CTX ctx) {
        Optional<WorldPosition> optionalWorldPosition = ctx.getOptionalParameter(DirectContextParameters.POSITION);
        if (optionalWorldPosition.isPresent()) {
            World world = optionalWorldPosition.get().world();
            ExistingBlock blockAt = world.getBlock(MiscUtils.floor(this.x.getDouble(ctx)), MiscUtils.floor(this.y.getDouble(ctx)), MiscUtils.floor(this.z.getDouble(ctx)));
            return MiscUtils.matchRegex(blockAt.id().asString(), this.ids, this.regexMatch);
        }
        return false;
    }

    public static <CTX extends Context> ConditionFactory<CTX, MatchBlockCondition<CTX>> factory() {
        return new Factory<>();
    }

    private static class Factory<CTX extends Context> implements ConditionFactory<CTX, MatchBlockCondition<CTX>> {
        private static final String[] ID = new String[] {"id", "block", "blocks"};

        @Override
        public MatchBlockCondition<CTX> create(ConfigSection section) {
            return new MatchBlockCondition<>(
                    section.getNonEmptyList(ID, ConfigValue::getAsString),
                    section.getBoolean("regex"),
                    section.getNumber("x", ConfigConstants.POSITION_X),
                    section.getNumber("y", ConfigConstants.POSITION_Y),
                    section.getNumber("z", ConfigConstants.POSITION_Z)
            );
        }
    }
}