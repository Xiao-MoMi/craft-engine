package net.momirealms.craftengine.core.loot.function;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.List;
import java.util.function.BiFunction;

public final class LootFunctions {
    public static final LootFunctionType<ApplyBonusCountFunction> APPLY_BONUS = register(Key.ce("apply_bonus"), ApplyBonusCountFunction.FACTORY);
    public static final LootFunctionType<ApplyDataFunction> APPLY_DATA = register(Key.ce("apply_data"), ApplyDataFunction.FACTORY);
    public static final LootFunctionType<SetCountFunction> SET_COUNT = register(Key.ce("set_count"), SetCountFunction.FACTORY);
    public static final LootFunctionType<ExplosionDecayFunction> EXPLOSION_DECAY = register(Key.ce("explosion_decay"), ExplosionDecayFunction.FACTORY);
    public static final LootFunctionType<DropExpFunction> DROP_EXP = register(Key.ce("drop_exp"), DropExpFunction.FACTORY);
    public static final LootFunctionType<LimitCountFunction> LIMIT_COUNT = register(Key.ce("limit_count"), LimitCountFunction.FACTORY);

    private LootFunctions() {}

    public static <T extends LootFunction> LootFunctionType<T> register(Key key, LootFunctionFactory<T> factory) {
        LootFunctionType<T> type = new LootFunctionType<>(key, factory);
        ((WritableRegistry<LootFunctionType<? extends LootFunction>>) BuiltInRegistries.LOOT_FUNCTION_TYPE)
                .register(ResourceKey.create(Registries.LOOT_FUNCTION_TYPE.location(), key), type);
        return type;
    }

    public static BiFunction<Item, LootContext, Item> identity() {
        return (item, context) -> item;
    }

    public static BiFunction<Item, LootContext, Item> compose(List<? extends BiFunction<Item, LootContext, Item>> terms) {
        List<BiFunction<Item, LootContext, Item>> list = List.copyOf(terms);
        return switch (list.size()) {
            case 0 -> identity();
            case 1 -> list.get(0);
            case 2 -> {
                BiFunction<Item, LootContext, Item> f1 = list.get(0);
                BiFunction<Item, LootContext, Item> f2 = list.get(1);
                yield (item, context) -> f2.apply(f1.apply(item, context), context);
            }
            default -> (item, context) -> {
                for (BiFunction<Item, LootContext, Item> function : list) {
                    item = function.apply(item, context);
                }
                return item;
            };
        };
    }

    public static LootFunction fromConfig(ConfigValue value) {
        return fromConfig(value.getAsSection());
    }

    public static LootFunction fromConfig(ConfigSection section) {
        String type = section.getNonEmptyString("type");
        Key key = Key.ce(type);
        LootFunctionType<? extends LootFunction> functionType = BuiltInRegistries.LOOT_FUNCTION_TYPE.getValue(key);
        if (functionType == null) {
            throw new KnownResourceException("loot.function.unknown_type", section.assemblePath("type"), key.asString());
        }
        return functionType.factory().create(section);
    }
}
