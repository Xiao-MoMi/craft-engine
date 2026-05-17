package net.momirealms.craftengine.core.plugin.context.condition;

import net.momirealms.craftengine.core.entity.Entity;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public final class MatchEntityCondition<CTX extends Context> implements Condition<CTX> {
    private final Set<String> ids;
    private final boolean regexMatch;

    private MatchEntityCondition(Collection<String> ids, boolean regexMatch) {
        this.ids = new HashSet<>(ids);
        this.regexMatch = regexMatch;
    }

    @Override
    public boolean test(CTX ctx) {
        Optional<Entity> entity = ctx.getOptionalParameter(DirectContextParameters.ENTITY);
        return entity.filter(value -> MiscUtils.matchRegex(value.type().asString(), this.ids, this.regexMatch)).isPresent();
    }

    public static <CTX extends Context> ConditionFactory<CTX, MatchEntityCondition<CTX>> factory() {
        return new Factory<>();
    }

    private static class Factory<CTX extends Context> implements ConditionFactory<CTX, MatchEntityCondition<CTX>> {
        private static final String[] ID = new String[] {"id", "entity", "entities"};

        @Override
        public MatchEntityCondition<CTX> create(ConfigSection section) {
            return new MatchEntityCondition<>(
                    section.getNonNullStringList(ID),
                    section.getBoolean("regex")
            );
        }
    }
}