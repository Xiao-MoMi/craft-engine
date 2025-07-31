package net.momirealms.craftengine.core.registry;

import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.properties.PropertyFactory;
import net.momirealms.craftengine.core.entity.furniture.HitBoxFactory;
import net.momirealms.craftengine.core.item.ItemDataModifierFactory;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviorFactory;
import net.momirealms.craftengine.core.item.equipment.EquipmentFactory;
import net.momirealms.craftengine.core.item.recipe.CustomRecipeResult;
import net.momirealms.craftengine.core.item.recipe.CustomSmithingTransformRecipe;
import net.momirealms.craftengine.core.item.recipe.RecipeFactory;
import net.momirealms.craftengine.core.item.recipe.network.legacy.LegacyRecipe;
import net.momirealms.craftengine.core.item.recipe.network.modern.display.RecipeDisplay;
import net.momirealms.craftengine.core.item.recipe.network.modern.display.slot.SlotDisplay;
import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.loot.entry.LootEntryContainerFactory;
import net.momirealms.craftengine.core.loot.function.ApplyBonusCountFunction;
import net.momirealms.craftengine.core.loot.function.LootFunctionFactory;
import net.momirealms.craftengine.core.pack.conflict.PathContext;
import net.momirealms.craftengine.core.pack.conflict.resolution.ResolutionFactory;
import net.momirealms.craftengine.core.pack.host.ResourcePackHostFactory;
import net.momirealms.craftengine.core.pack.model.ItemModelFactory;
import net.momirealms.craftengine.core.pack.model.ItemModelReader;
import net.momirealms.craftengine.core.pack.model.condition.ConditionPropertyFactory;
import net.momirealms.craftengine.core.pack.model.condition.ConditionPropertyReader;
import net.momirealms.craftengine.core.pack.model.rangedisptach.RangeDispatchPropertyFactory;
import net.momirealms.craftengine.core.pack.model.rangedisptach.RangeDispatchPropertyReader;
import net.momirealms.craftengine.core.pack.model.select.SelectPropertyFactory;
import net.momirealms.craftengine.core.pack.model.select.SelectPropertyReader;
import net.momirealms.craftengine.core.pack.model.special.SpecialModelFactory;
import net.momirealms.craftengine.core.pack.model.special.SpecialModelReader;
import net.momirealms.craftengine.core.pack.model.tint.TintFactory;
import net.momirealms.craftengine.core.pack.model.tint.TintReader;
import net.momirealms.craftengine.core.plugin.config.template.TemplateArgumentFactory;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.plugin.context.condition.ConditionFactory;
import net.momirealms.craftengine.core.plugin.context.function.FunctionFactory;
import net.momirealms.craftengine.core.plugin.context.number.NumberProviderFactory;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelectorFactory;
import net.momirealms.craftengine.core.util.ResourceKey;

public class BuiltInRegistries {
    public static final Registry<CustomBlock> BLOCK = createDynamicBoundRegistry(Registries.BLOCK);
    public static final Registry<BlockBehaviorFactory> BLOCK_BEHAVIOR_FACTORY = createConstantBoundRegistry(Registries.BLOCK_BEHAVIOR_FACTORY);
    public static final Registry<ItemDataModifierFactory<?>> ITEM_DATA_MODIFIER_FACTORY = createConstantBoundRegistry(Registries.ITEM_DATA_MODIFIER_FACTORY);
    public static final Registry<ItemBehaviorFactory> ITEM_BEHAVIOR_FACTORY = createConstantBoundRegistry(Registries.ITEM_BEHAVIOR_FACTORY);
    public static final Registry<PropertyFactory> PROPERTY_FACTORY = createConstantBoundRegistry(Registries.PROPERTY_FACTORY);
    public static final Registry<LootFunctionFactory<?>> LOOT_FUNCTION_FACTORY = createConstantBoundRegistry(Registries.LOOT_FUNCTION_FACTORY);
    public static final Registry<ConditionFactory<LootContext>> LOOT_CONDITION_FACTORY = createConstantBoundRegistry(Registries.LOOT_CONDITION_FACTORY);
    public static final Registry<LootEntryContainerFactory<?>> LOOT_ENTRY_CONTAINER_FACTORY = createConstantBoundRegistry(Registries.LOOT_ENTRY_CONTAINER_FACTORY);
    public static final Registry<NumberProviderFactory> NUMBER_PROVIDER_FACTORY = createConstantBoundRegistry(Registries.NUMBER_PROVIDER_FACTORY);
    public static final Registry<TemplateArgumentFactory> TEMPLATE_ARGUMENT_FACTORY = createConstantBoundRegistry(Registries.TEMPLATE_ARGUMENT_FACTORY);
    public static final Registry<ItemModelFactory> ITEM_MODEL_FACTORY = createConstantBoundRegistry(Registries.ITEM_MODEL_FACTORY);
    public static final Registry<ItemModelReader> ITEM_MODEL_READER = createConstantBoundRegistry(Registries.ITEM_MODEL_READER);
    public static final Registry<TintFactory> TINT_FACTORY = createConstantBoundRegistry(Registries.TINT_FACTORY);
    public static final Registry<TintReader> TINT_READER = createConstantBoundRegistry(Registries.TINT_READER);
    public static final Registry<SpecialModelFactory> SPECIAL_MODEL_FACTORY = createConstantBoundRegistry(Registries.SPECIAL_MODEL_FACTORY);
    public static final Registry<SpecialModelReader> SPECIAL_MODEL_READER = createConstantBoundRegistry(Registries.SPECIAL_MODEL_READER);
    public static final Registry<RangeDispatchPropertyFactory> RANGE_DISPATCH_PROPERTY_FACTORY = createConstantBoundRegistry(Registries.RANGE_DISPATCH_PROPERTY_FACTORY);
    public static final Registry<RangeDispatchPropertyReader> RANGE_DISPATCH_PROPERTY_READER = createConstantBoundRegistry(Registries.RANGE_DISPATCH_PROPERTY_READER);
    public static final Registry<ConditionPropertyFactory> CONDITION_PROPERTY_FACTORY = createConstantBoundRegistry(Registries.CONDITION_PROPERTY_FACTORY);
    public static final Registry<ConditionPropertyReader> CONDITION_PROPERTY_READER = createConstantBoundRegistry(Registries.CONDITION_PROPERTY_READER);
    public static final Registry<SelectPropertyFactory> SELECT_PROPERTY_FACTORY = createConstantBoundRegistry(Registries.SELECT_PROPERTY_FACTORY);
    public static final Registry<SelectPropertyReader> SELECT_PROPERTY_READER = createConstantBoundRegistry(Registries.SELECT_PROPERTY_READER);
    public static final Registry<RecipeFactory<?>> RECIPE_FACTORY = createConstantBoundRegistry(Registries.RECIPE_FACTORY);
    public static final Registry<ApplyBonusCountFunction.FormulaFactory> FORMULA_FACTORY = createConstantBoundRegistry(Registries.FORMULA_FACTORY);
    public static final Registry<ConditionFactory<PathContext>> PATH_MATCHER_FACTORY = createConstantBoundRegistry(Registries.PATH_MATCHER_FACTORY);
    public static final Registry<ResolutionFactory> RESOLUTION_FACTORY = createConstantBoundRegistry(Registries.RESOLUTION_FACTORY);
    public static final Registry<CustomSmithingTransformRecipe.ItemDataProcessor.ProcessorFactory> SMITHING_RESULT_PROCESSOR_FACTORY = createConstantBoundRegistry(Registries.SMITHING_RESULT_PROCESSOR_FACTORY);
    public static final Registry<HitBoxFactory> HITBOX_FACTORY = createConstantBoundRegistry(Registries.HITBOX_FACTORY);
    public static final Registry<ResourcePackHostFactory> RESOURCE_PACK_HOST_FACTORY = createConstantBoundRegistry(Registries.RESOURCE_PACK_HOST_FACTORY);
    public static final Registry<FunctionFactory<PlayerOptionalContext>> EVENT_FUNCTION_FACTORY = createConstantBoundRegistry(Registries.EVENT_FUNCTION_FACTORY);
    public static final Registry<ConditionFactory<PlayerOptionalContext>> EVENT_CONDITION_FACTORY = createConstantBoundRegistry(Registries.EVENT_CONDITION_FACTORY);
    public static final Registry<PlayerSelectorFactory<?>> PLAYER_SELECTOR_FACTORY = createConstantBoundRegistry(Registries.PLAYER_SELECTOR_FACTORY);
    public static final Registry<EquipmentFactory> EQUIPMENT_FACTORY = createConstantBoundRegistry(Registries.EQUIPMENT_FACTORY);
    public static final Registry<SlotDisplay.Type> SLOT_DISPLAY_TYPE = createConstantBoundRegistry(Registries.SLOT_DISPLAY_TYPE);
    public static final Registry<RecipeDisplay.Type> RECIPE_DISPLAY_TYPE = createConstantBoundRegistry(Registries.RECIPE_DISPLAY_TYPE);
    public static final Registry<LegacyRecipe.Type> LEGACY_RECIPE_TYPE = createConstantBoundRegistry(Registries.LEGACY_RECIPE_TYPE);
    public static final Registry<CustomRecipeResult.ApplyItemDataProcessor.Type<?>> RECIPE_POST_PROCESSOR_TYPE = createConstantBoundRegistry(Registries.RECIPE_POST_PROCESSOR_TYPE);

    private static <T> Registry<T> createConstantBoundRegistry(ResourceKey<? extends Registry<T>> key) {
        return new ConstantBoundRegistry<>(key);
    }

    private static <T> Registry<T> createDynamicBoundRegistry(ResourceKey<? extends Registry<T>> key) {
        return new DynamicBoundRegistry<>(key);
    }
}
