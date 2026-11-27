package net.momirealms.craftengine.core.attribute.modifier.removal;

import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;

public record ContextRemovalCondition(Condition<Context> condition, int checkInterval) implements RemovalCondition {

    public ContextRemovalCondition {
        if (checkInterval <= 0) {
            throw new IllegalArgumentException("Check interval must be positive");
        }
    }

    @Override
    public boolean test(RemovalContext context) {
        return this.condition.test(context.context());
    }

    @Override
    public long nextCheckTick(RemovalContext context) {
        return context.currentTick() + this.checkInterval;
    }
}
