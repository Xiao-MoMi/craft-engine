package net.momirealms.craftengine.core.attribute.damage.effect;

import net.momirealms.craftengine.core.attribute.damage.DamageEvent;
import net.momirealms.craftengine.core.entity.LivingEntity;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;

public final class LifeStealDamageEffect implements DamageEffect {
    public static final DamageEffectFactory<LifeStealDamageEffect> FACTORY = section -> new LifeStealDamageEffect(
            section.getNumber("ratio", ConfigConstants.CONSTANT_ZERO),
            section.getNumber("amount", ConfigConstants.CONSTANT_ZERO)
    );

    private final NumberProvider ratio;
    private final NumberProvider amount;

    public LifeStealDamageEffect(NumberProvider ratio, NumberProvider amount) {
        this.ratio = ratio;
        this.amount = amount;
    }

    @Override
    public void apply(DamageEvent event) {
        double finalDamage = event.finalDamage();
        if (!(finalDamage > 0) || !(event.source().causingEntity() instanceof LivingEntity attacker)) {
            return;
        }
        double healing = finalDamage * this.ratio.getDouble(event.context()) + this.amount.getDouble(event.context());
        if (healing > 0 && Double.isFinite(healing)) {
            attacker.heal(healing);
        }
    }
}
