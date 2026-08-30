package net.momirealms.craftengine.core.attribute.damage;

import net.momirealms.craftengine.core.attribute.damage.effect.DamageEffect;
import net.momirealms.craftengine.core.attribute.formula.DamageFormula;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record DamageRule(@Nullable DamageFormula formula, List<DamageEffect> effects) {

    public DamageRule {
        effects = List.copyOf(effects);
    }
}
