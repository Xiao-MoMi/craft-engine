package net.momirealms.craftengine.core.attribute.damage.effect;

import net.momirealms.craftengine.core.util.Key;

public record DamageEffectType<T extends DamageEffect>(Key id, DamageEffectFactory<T> factory) {
}
