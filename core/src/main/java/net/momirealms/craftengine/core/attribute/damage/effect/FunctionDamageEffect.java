package net.momirealms.craftengine.core.attribute.damage.effect;

import net.momirealms.craftengine.core.attribute.damage.DamageEvent;

public final class FunctionDamageEffect implements DamageEffect {
    public static final FunctionDamageEffect INSTANCE = new FunctionDamageEffect();
    public static final DamageEffectFactory<FunctionDamageEffect> FACTORY = section -> INSTANCE;

    private FunctionDamageEffect() {
    }

    @Override
    public void apply(DamageEvent event) {
    }
}
