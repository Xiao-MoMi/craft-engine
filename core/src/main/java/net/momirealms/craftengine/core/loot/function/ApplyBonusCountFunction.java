package net.momirealms.craftengine.core.loot.function;

import net.momirealms.craftengine.core.item.Enchantment;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.loot.condition.LootCondition;
import net.momirealms.craftengine.core.loot.condition.LootConditions;
import net.momirealms.craftengine.core.loot.parameter.LootParameters;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class ApplyBonusCountFunction<T> extends AbstractLootConditionalFunction<T> {
    public static final Factory<?> FACTORY = new Factory<>();
    private final Key enchantment;
    private final Formula formula;

    public ApplyBonusCountFunction(List<LootCondition> predicates, Key enchantment, Formula formula) {
        super(predicates);
        this.enchantment = enchantment;
        this.formula = formula;
    }

    @Override
    protected Item<T> applyInternal(Item<T> item, LootContext context) {
        Optional<Item<?>> itemInHand = context.getOptionalParameter(LootParameters.TOOL);
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
            String enchantment = (String) arguments.get("enchantment");
            if (enchantment == null || enchantment.isEmpty()) {
                throw new IllegalArgumentException("enchantment is required");
            }
            Map<String, Object> formulaMap = MiscUtils.castToMap(arguments.get("formula"), true);
            if (formulaMap == null) {
                throw new IllegalArgumentException("formula is required");
            }
            List<LootCondition> conditions = Optional.ofNullable(arguments.get("conditions"))
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

        static {
            register(ORE_DROPS, OreDrops.FACTORY);
        }

        public static void register(Key key, FormulaFactory factory) {
            Holder.Reference<FormulaFactory> holder = ((WritableRegistry<FormulaFactory>) BuiltInRegistries.FORMULA_FACTORY)
                    .registerForHolder(new ResourceKey<>(Registries.FORMULA_FACTORY.location(), key));
            holder.bindValue(factory);
        }

        public static Formula fromMap(Map<String, Object> map) {
            String type = (String) map.get("type");
            if (type == null) {
                throw new NullPointerException("number type cannot be null");
            }
            Key key = Key.withDefaultNamespace(type, "craftengine");
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
                int i = ThreadLocalRandom.current().nextInt(enchantmentLevel + 2) - 1;
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
}
