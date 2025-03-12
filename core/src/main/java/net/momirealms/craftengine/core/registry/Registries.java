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
import net.momirealms.craftengine.core.pack.conflict.matcher.PathMatcherFactory;
import net.momirealms.craftengine.core.pack.conflict.resolution.ResolutionFactory;
import net.momirealms.craftengine.core.pack.model.ItemModelFactory;
import net.momirealms.craftengine.core.pack.model.condition.ConditionPropertyFactory;
import net.momirealms.craftengine.core.pack.model.rangedisptach.RangeDispatchPropertyFactory;
import net.momirealms.craftengine.core.pack.model.select.SelectPropertyFactory;
import net.momirealms.craftengine.core.pack.model.special.SpecialModelFactory;
import net.momirealms.craftengine.core.pack.model.tint.TintFactory;
import net.momirealms.craftengine.core.plugin.config.template.TemplateArgumentFactory;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

public class Registries {
    public static final Key ROOT_REGISTRY = Key.withDefaultNamespace("root");
    public static final ResourceKey<Registry<CustomBlock>> BLOCK = new ResourceKey<>(ROOT_REGISTRY, Key.withDefaultNamespace("block"));
    public static final ResourceKey<Registry<Key>> OPTIMIZED_ITEM_ID = new ResourceKey<>(ROOT_REGISTRY, Key.withDefaultNamespace("optimized_item_id"));
    public static final ResourceKey<Registry<PropertyFactory>> PROPERTY_FACTORY = new ResourceKey<>(ROOT_REGISTRY, Key.withDefaultNamespace("property_factory"));
    public static final ResourceKey<Registry<BlockBehaviorFactory>> BLOCK_BEHAVIOR_FACTORY = new ResourceKey<>(ROOT_REGISTRY, Key.withDefaultNamespace("block_behavior_factory"));
    public static final ResourceKey<Registry<ItemBehaviorFactory>> ITEM_BEHAVIOR_FACTORY = new ResourceKey<>(ROOT_REGISTRY, Key.withDefaultNamespace("item_behavior_factory"));
    public static final ResourceKey<Registry<LootFunctionFactory<?>>> LOOT_FUNCTION_FACTORY = new ResourceKey<>(ROOT_REGISTRY, Key.withDefaultNamespace("loot_function_factory"));
    public static final ResourceKey<Registry<LootEntryContainerFactory<?>>> LOOT_ENTRY_CONTAINER_FACTORY = new ResourceKey<>(ROOT_REGISTRY, Key.withDefaultNamespace("loot_entry_container_factory"));
    public static final ResourceKey<Registry<LootConditionFactory>> LOOT_CONDITION_FACTORY = new ResourceKey<>(ROOT_REGISTRY, Key.withDefaultNamespace("loot_condition_factory"));
    public static final ResourceKey<Registry<NumberProviderFactory>> NUMBER_PROVIDER_FACTORY = new ResourceKey<>(ROOT_REGISTRY, Key.withDefaultNamespace("number_provider_factory"));
    public static final ResourceKey<Registry<TemplateArgumentFactory>> TEMPLATE_ARGUMENT_FACTORY = new ResourceKey<>(ROOT_REGISTRY, Key.withDefaultNamespace("template_argument_factory"));
    public static final ResourceKey<Registry<ItemModelFactory>> ITEM_MODEL_FACTORY = new ResourceKey<>(ROOT_REGISTRY, Key.withDefaultNamespace("item_model_factory"));
    public static final ResourceKey<Registry<TintFactory>> TINT_FACTORY = new ResourceKey<>(ROOT_REGISTRY, Key.withDefaultNamespace("tint_factory"));
    public static final ResourceKey<Registry<SpecialModelFactory>> SPECIAL_MODEL_FACTORY = new ResourceKey<>(ROOT_REGISTRY, Key.withDefaultNamespace("special_model_factory"));
    public static final ResourceKey<Registry<RangeDispatchPropertyFactory>> RANGE_DISPATCH_PROPERTY_FACTORY = new ResourceKey<>(ROOT_REGISTRY, Key.withDefaultNamespace("range_dispatch_property_factory"));
    public static final ResourceKey<Registry<ConditionPropertyFactory>> CONDITION_PROPERTY_FACTORY = new ResourceKey<>(ROOT_REGISTRY, Key.withDefaultNamespace("condition_property_factory"));
    public static final ResourceKey<Registry<SelectPropertyFactory>> SELECT_PROPERTY_FACTORY = new ResourceKey<>(ROOT_REGISTRY, Key.withDefaultNamespace("select_property_factory"));
    public static final ResourceKey<Registry<RecipeFactory<?>>> RECIPE_FACTORY = new ResourceKey<>(ROOT_REGISTRY, Key.withDefaultNamespace("recipe_factory"));
    public static final ResourceKey<Registry<ApplyBonusCountFunction.FormulaFactory>> FORMULA_FACTORY = new ResourceKey<>(ROOT_REGISTRY, Key.withDefaultNamespace("formula_factory"));
    public static final ResourceKey<Registry<PathMatcherFactory>> PATH_MATCHER_FACTORY = new ResourceKey<>(ROOT_REGISTRY, Key.withDefaultNamespace("path_matcher_factory"));
    public static final ResourceKey<Registry<ResolutionFactory>> RESOLUTION_FACTORY = new ResourceKey<>(ROOT_REGISTRY, Key.withDefaultNamespace("resolution_factory"));
}
