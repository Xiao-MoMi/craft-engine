package net.momirealms.craftengine.core.plugin.context;

import net.momirealms.craftengine.core.plugin.context.condition.ConditionFactory;
import net.momirealms.craftengine.core.util.Key;

public final class CommonConditionType<T extends Condition<Context>> extends ConditionType<Context, T> {

    public CommonConditionType(Key id, ConditionFactory<Context, T> factory) {
        super(id, factory);
    }
}
