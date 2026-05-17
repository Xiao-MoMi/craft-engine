package net.momirealms.craftengine.core.plugin.context.condition;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public final class MatchItemCondition<CTX extends Context> implements Condition<CTX> {
    private final Set<String> ids;
    private final boolean regexMatch;

    private MatchItemCondition(Collection<String> ids, boolean regexMatch) {
        this.ids = new HashSet<>(ids);
        this.regexMatch = regexMatch;
    }

    @Override
    public boolean test(CTX ctx) {
        Optional<Item> item = ctx.getOptionalParameter(DirectContextParameters.ITEM_IN_HAND);
        return item.filter(value -> MiscUtils.matchRegex(value.id().asString(), this.ids, this.regexMatch)).isPresent();
    }

    public static <CTX extends Context> ConditionFactory<CTX, MatchItemCondition<CTX>> factory() {
        return new Factory<>();
    }

    private static class Factory<CTX extends Context> implements ConditionFactory<CTX, MatchItemCondition<CTX>> {
        private static final String[] ID = new String[] {"id", "item", "items"};

        @Override
        public MatchItemCondition<CTX> create(ConfigSection section) {
            return new MatchItemCondition<>(section.getNonNullStringList(ID), section.getBoolean("regex"));
        }
    }
}