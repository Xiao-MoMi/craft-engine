package net.momirealms.craftengine.core.registry;

import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.properties.PropertyFactory;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviorFactory;
import net.momirealms.craftengine.core.item.recipe.RecipeFactory;
import net.momirealms.craftengine.core.loot.condition.LootConditionFactory;
import net.momirealms.craftengine.core.loot.entry.LootEntryContainerFactory;
import net.momirealms.craftengine.core.loot.function.ApplyBonusCountFunction;
import net.momirealms.craftengine.core.loot.function.LootFunctionFactory;
import net.momirealms.craftengine.core.loot.number.NumberProviderFactory;
import net.momirealms.craftengine.core.pack.model.ItemModelFactory;
import net.momirealms.craftengine.core.pack.model.condition.ConditionPropertyFactory;
import net.momirealms.craftengine.core.pack.model.rangedisptach.RangeDispatchPropertyFactory;
import net.momirealms.craftengine.core.pack.model.select.SelectPropertyFactory;
import net.momirealms.craftengine.core.pack.model.special.SpecialModelFactory;
import net.momirealms.craftengine.core.pack.model.tint.TintFactory;
import net.momirealms.craftengine.core.plugin.config.template.TemplateArgumentFactory;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

public class BuiltInRegistries {
    public static final Registry<CustomBlock> BLOCK = createRegistry(Registries.BLOCK);
    public static final Registry<Key> OPTIMIZED_ITEM_ID = createRegistry(Registries.OPTIMIZED_ITEM_ID);
    public static final Registry<PropertyFactory> PROPERTY_FACTORY = createRegistry(Registries.PROPERTY_FACTORY);
    public static final Registry<BlockBehaviorFactory> BLOCK_BEHAVIOR_FACTORY = createRegistry(Registries.BLOCK_BEHAVIOR_FACTORY);
    public static final Registry<LootFunctionFactory<?>> LOOT_FUNCTION_FACTORY = createRegistry(Registries.LOOT_FUNCTION_FACTORY);
    public static final Registry<LootConditionFactory> LOOT_CONDITION_FACTORY = createRegistry(Registries.LOOT_CONDITION_FACTORY);
    public static final Registry<LootEntryContainerFactory<?>> LOOT_ENTRY_CONTAINER_FACTORY = createRegistry(Registries.LOOT_ENTRY_CONTAINER_FACTORY);
    public static final Registry<NumberProviderFactory> NUMBER_PROVIDER_FACTORY = createRegistry(Registries.NUMBER_PROVIDER_FACTORY);
    public static final Registry<ItemBehaviorFactory> ITEM_BEHAVIOR_FACTORY = createRegistry(Registries.ITEM_BEHAVIOR_FACTORY);
    public static final Registry<TemplateArgumentFactory> TEMPLATE_ARGUMENT_FACTORY = createRegistry(Registries.TEMPLATE_ARGUMENT_FACTORY);
    public static final Registry<ItemModelFactory> ITEM_MODEL_FACTORY = createRegistry(Registries.ITEM_MODEL_FACTORY);
    public static final Registry<TintFactory> TINT_FACTORY = createRegistry(Registries.TINT_FACTORY);
    public static final Registry<SpecialModelFactory> SPECIAL_MODEL_FACTORY = createRegistry(Registries.SPECIAL_MODEL_FACTORY);
    public static final Registry<RangeDispatchPropertyFactory> RANGE_DISPATCH_PROPERTY_FACTORY = createRegistry(Registries.RANGE_DISPATCH_PROPERTY_FACTORY);
    public static final Registry<ConditionPropertyFactory> CONDITION_PROPERTY_FACTORY = createRegistry(Registries.CONDITION_PROPERTY_FACTORY);
    public static final Registry<SelectPropertyFactory> SELECT_PROPERTY_FACTORY = createRegistry(Registries.SELECT_PROPERTY_FACTORY);
    public static final Registry<RecipeFactory<?>> RECIPE_FACTORY = createRegistry(Registries.RECIPE_FACTORY);
    public static final Registry<ApplyBonusCountFunction.FormulaFactory> FORMULA_FACTORY = createRegistry(Registries.FORMULA_FACTORY);

    private static <T> Registry<T> createRegistry(ResourceKey<? extends Registry<T>> key) {
        return new MappedRegistry<>(key);
    }
}
