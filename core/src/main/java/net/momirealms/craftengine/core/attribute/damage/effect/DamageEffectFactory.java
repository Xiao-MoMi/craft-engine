package net.momirealms.craftengine.core.attribute.damage.effect;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;

@FunctionalInterface
public interface DamageEffectFactory<T extends DamageEffect> {

    T create(ConfigSection section);
}
