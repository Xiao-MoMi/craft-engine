package net.momirealms.craftengine.core.loot.function;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.data.Enchantment;
import net.momirealms.craftengine.core.loot.LootConditions;
import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ApplyBonusCountFunction<T> extends AbstractLootConditionalFunction<T> {
    public static final Factory<?> FACTORY = new Factory<>();
    private final Key enchantment;
    private final Formula formula;

    public ApplyBonusCountFunction(List<Condition<LootContext>> predicates, Key enchantment, Formula formula) {
        super(predicates);
        this.enchantment = enchantment;
        this.formula = formula;
    }

    @Override
    protected Item<T> applyInternal(Item<T> item, LootContext context) {
        Optional<Item<?>> itemInHand = context.getOptionalParameter(DirectContextParameters.ITEM_IN_HAND);
        int level = itemInHand.map(value -> value.getEnchantment(this.enchantment).map(Enchantment::level).orElse(0)).orElse(0);
        int newCount = this.formula.apply(item.count(), level);
        item.count(newCount);
        return item;
    }

    @Override
    public Key type() {
        return LootFunctions.APPLY_BONUS;
    }

    public static class Factory<T> implements LootFunctionFactory<T> {

        @SuppressWarnings("unchecked")
        @Override
        public LootFunction<T> create(Map<String, Object> arguments) {
            String enchantment = ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("enchantment"), "warning.config.loot_table.function.apply_bonus.missing_enchantment");
            Map<String, Object> formulaMap = MiscUtils.castToMap(arguments.get("formula"), true);
            if (formulaMap == null) {
                throw new LocalizedResourceConfigException("warning.config.loot_table.function.apply_bonus.missing_formula");
            }
            List<Condition<LootContext>> conditions = Optional.ofNullable(arguments.get("conditions"))
                    .map(it -> LootConditions.fromMapList((List<Map<String, Object>>) it))
                    .orElse(Collections.emptyList());
            return new ApplyBonusCountFunction<>(conditions, Key.from(enchantment), Formulas.fromMap(formulaMap));
        }
    }

    public interface Formula {
        int apply(int initialCount, int enchantmentLevel);

        Key type();
    }

    public interface FormulaFactory {

        Formula create(Map<String, Object> arguments);
    }

    public static class Formulas {
        public static final Key ORE_DROPS = Key.of("craftengine:ore_drops");
        public static final Key CROP_DROPS = Key.of("craftengine:binomial_with_bonus_count");

        static {
            register(ORE_DROPS, OreDrops.FACTORY);
            register(CROP_DROPS, CropDrops.FACTORY);
        }

        public static void register(Key key, FormulaFactory factory) {
            ((WritableRegistry<FormulaFactory>) BuiltInRegistries.FORMULA_FACTORY)
                    .register(ResourceKey.create(Registries.FORMULA_FACTORY.location(), key), factory);
        }

        public static Formula fromMap(Map<String, Object> map) {
            String type = (String) map.get("type");
            if (type == null) {
                throw new NullPointerException("number type cannot be null");
            }
            Key key = Key.withDefaultNamespace(type, Key.DEFAULT_NAMESPACE);
            FormulaFactory factory = BuiltInRegistries.FORMULA_FACTORY.getValue(key);
            if (factory == null) {
                throw new IllegalArgumentException("Unknown formula type: " + type);
            }
            return factory.create(map);
        }
    }

    public static class OreDrops implements Formula {
        public static final Factory FACTORY = new Factory();
        private static final OreDrops INSTANCE = new OreDrops();

        @Override
        public int apply(int initialCount, int enchantmentLevel) {
            if (enchantmentLevel > 0) {
                int i = RandomUtils.generateRandomInt(0, enchantmentLevel + 2) - 1;
                if (i < 0) {
                    i = 0;
                }
                return initialCount * (i + 1);
            } else {
                return initialCount;
            }
        }

        @Override
        public Key type() {
            return Formulas.ORE_DROPS;
        }

        public static class Factory implements FormulaFactory {

            @Override
            public Formula create(Map<String, Object> arguments) {
                return INSTANCE;
            }
        }
    }

    public static class CropDrops implements Formula {
        public static final Factory FACTORY = new Factory();
        private final int extra;
        private final float probability;

        public CropDrops(int extra, float probability) {
            this.extra = extra;
            this.probability = probability;
        }

        @Override
        public int apply(int initialCount, int enchantmentLevel) {
            for (int i = 0; i < enchantmentLevel + this.extra; i++) {
                if (RandomUtils.generateRandomFloat(0,1) < this.probability) {
                    initialCount++;
                }
            }
            return initialCount;
        }

        @Override
        public Key type() {
            return Formulas.CROP_DROPS;
        }

        public static class Factory implements FormulaFactory {

            @Override
            public Formula create(Map<String, Object> arguments) {
                int extra = ResourceConfigUtils.getAsInt(arguments.getOrDefault("extra", 1), "extra");
                float probability = ResourceConfigUtils.getAsFloat(arguments.getOrDefault("probability", 0.5f), "probability");
                return new CropDrops(extra, probability);
            }
        }
    }
}
