package net.momirealms.craftengine.core.pack.conflict.matcher;

import net.momirealms.craftengine.core.pack.conflict.PathContext;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.ConditionType;
import net.momirealms.craftengine.core.plugin.context.condition.ConditionFactory;
import net.momirealms.craftengine.core.util.Key;

public final class PathMatcherType<T extends Condition<PathContext>> extends ConditionType<PathContext, T> {

    public PathMatcherType(Key id, ConditionFactory<PathContext, T> factory) {
        super(id, factory);
    }
}
