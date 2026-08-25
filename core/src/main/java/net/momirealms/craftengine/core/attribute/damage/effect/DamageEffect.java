package net.momirealms.craftengine.core.attribute.damage.effect;

import net.momirealms.craftengine.core.attribute.damage.DamageEvent;

@FunctionalInterface
public interface DamageEffect {

    void apply(DamageEvent event);
}
