package net.momirealms.craftengine.core.attribute.formula;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.Map;

public final class DamageFormulas {
    public static final DamageFormulaType<ExpressionDamageFormula> EXPRESSION = register(Key.ce("expression"), ExpressionDamageFormula.FACTORY);
    public static final DamageFormulaType<CompositionDamageFormula> COMPOSITION = register(Key.ce("composition"), CompositionDamageFormula.FACTORY);
    public static final DamageFormulaType<JsDamageFormula> JS = register(Key.ce("js"), JsDamageFormula.FACTORY);

    private DamageFormulas() {}

    public static <T extends DamageFormula> DamageFormulaType<T> register(Key key, DamageFormulaFactory<T> factory) {
        DamageFormulaType<T> type = new DamageFormulaType<>(key, factory);
        ((WritableRegistry<DamageFormulaType<? extends DamageFormula>>) BuiltInRegistries.DAMAGE_FORMULA_TYPE)
                .register(ResourceKey.create(Registries.DAMAGE_FORMULA_TYPE.location(), key), type);
        return type;
    }

    public static DamageFormula fromConfig(ConfigValue value) {
        if (value.is(Map.class)) {
            return fromConfig(value.getAsSection());
        }
        return ExpressionDamageFormula.compile(value.path(), value.getAsString());
    }

    public static DamageFormula fromConfig(ConfigSection section) {
        String type = section.getString("type", "expression");
        Key key = Key.ce(type);
        DamageFormulaType<? extends DamageFormula> formulaType = BuiltInRegistries.DAMAGE_FORMULA_TYPE.getValue(key);
        if (formulaType == null) {
            throw new KnownResourceException("attribute.damage_formula.unknown_type", section.assemblePath("type"), type);
        }
        return formulaType.factory().create(section);
    }
}
