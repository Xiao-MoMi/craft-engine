package net.momirealms.craftengine.core.attribute.modifier.removal;

import java.util.function.Predicate;

public interface RemovalCondition extends Predicate<RemovalContext> {

    @Override
    boolean test(RemovalContext context);

    long nextCheckTick(RemovalContext context);
}
