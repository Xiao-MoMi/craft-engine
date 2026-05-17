package net.momirealms.craftengine.core.registry;

import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.behavior.BlockBehavior;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorType;
import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElement;
import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElementConfigType;
import net.momirealms.craftengine.core.block.entity.render.tint.BlockEntityTintSource;
import net.momirealms.craftengine.core.block.entity.render.tint.BlockEntityTintSourceType;
import net.momirealms.craftengine.core.block.property.PropertyType;
import net.momirealms.craftengine.core.block.setting.BlockSettingsModifier;
import net.momirealms.craftengine.core.block.setting.BlockSettingsModifierType;
import net.momirealms.craftengine.core.entity.furniture.behavior.FurnitureBehaviorTemplate;
import net.momirealms.craftengine.core.entity.furniture.behavior.FurnitureBehaviorType;
import net.momirealms.craftengine.core.entity.furniture.element.FurnitureElement;
import net.momirealms.craftengine.core.entity.furniture.element.FurnitureElementConfigType;
import net.momirealms.craftengine.core.entity.furniture.element.tint.FurnitureTintSource;
import net.momirealms.craftengine.core.entity.furniture.element.tint.FurnitureTintSourceType;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBox;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitboxConfigType;
import net.momirealms.craftengine.core.entity.furniture.setting.FurnitureSettingsModifier;
import net.momirealms.craftengine.core.entity.furniture.setting.FurnitureSettingsModifierType;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviorType;
import net.momirealms.craftengine.core.item.equipment.Equipment;
import net.momirealms.craftengine.core.item.equipment.EquipmentType;
import net.momirealms.craftengine.core.item.processor.ItemProcessor;
import net.momirealms.craftengine.core.item.processor.ItemProcessorType;
import net.momirealms.craftengine.core.item.recipe.Recipe;
import net.momirealms.craftengine.core.item.recipe.RecipeSerializer;
import net.momirealms.craftengine.core.item.recipe.remainder.CraftRemainder;
import net.momirealms.craftengine.core.item.recipe.remainder.CraftRemainderType;
import net.momirealms.craftengine.core.item.recipe.result.PostProcessor;
import net.momirealms.craftengine.core.item.recipe.result.PostProcessorType;
import net.momirealms.craftengine.core.item.recipe.transform.ItemTransformDataProcessor;
import net.momirealms.craftengine.core.item.setting.ItemSettingsModifier;
import net.momirealms.craftengine.core.item.setting.ItemSettingsModifierType;
import net.momirealms.craftengine.core.item.updater.ItemUpdater;
import net.momirealms.craftengine.core.item.updater.ItemUpdaterType;
import net.momirealms.craftengine.core.loot.entry.LootEntryContainer;
import net.momirealms.craftengine.core.loot.entry.LootEntryContainerType;
import net.momirealms.craftengine.core.loot.function.LootFunction;
import net.momirealms.craftengine.core.loot.function.LootFunctionType;
import net.momirealms.craftengine.core.loot.function.formula.Formula;
import net.momirealms.craftengine.core.loot.function.formula.FormulaType;
import net.momirealms.craftengine.core.pack.conflict.PathContext;
import net.momirealms.craftengine.core.pack.conflict.matcher.PathMatcherType;
import net.momirealms.craftengine.core.pack.conflict.resolution.Resolution;
import net.momirealms.craftengine.core.pack.conflict.resolution.ResolutionType;
import net.momirealms.craftengine.core.pack.host.ResourcePackHost;
import net.momirealms.craftengine.core.pack.host.ResourcePackHostType;
import net.momirealms.craftengine.core.pack.model.definition.ItemModel;
import net.momirealms.craftengine.core.pack.model.definition.ItemModelType;
import net.momirealms.craftengine.core.pack.model.definition.condition.ConditionProperty;
import net.momirealms.craftengine.core.pack.model.definition.condition.ConditionPropertyType;
import net.momirealms.craftengine.core.pack.model.definition.rangedisptach.RangeDispatchProperty;
import net.momirealms.craftengine.core.pack.model.definition.rangedisptach.RangeDispatchPropertyType;
import net.momirealms.craftengine.core.pack.model.definition.select.SelectProperty;
import net.momirealms.craftengine.core.pack.model.definition.select.SelectPropertyType;
import net.momirealms.craftengine.core.pack.model.definition.special.SpecialModel;
import net.momirealms.craftengine.core.pack.model.definition.special.SpecialModelType;
import net.momirealms.craftengine.core.pack.model.definition.tint.Tint;
import net.momirealms.craftengine.core.pack.model.definition.tint.TintType;
import net.momirealms.craftengine.core.plugin.config.template.argument.TemplateArgument;
import net.momirealms.craftengine.core.plugin.config.template.argument.TemplateArgumentType;
import net.momirealms.craftengine.core.plugin.context.CommonConditionType;
import net.momirealms.craftengine.core.plugin.context.CommonFunctionType;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.function.Function;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.number.NumberProviderType;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelectorType;
import net.momirealms.craftengine.core.plugin.network.codec.NetworkCodec;
import net.momirealms.craftengine.core.plugin.network.mod.ModPacket;
import net.momirealms.craftengine.core.plugin.network.protocol.chat.ClickEvent;
import net.momirealms.craftengine.core.plugin.network.protocol.dialog.Dialog;
import net.momirealms.craftengine.core.plugin.network.protocol.dialog.action.DialogAction;
import net.momirealms.craftengine.core.plugin.network.protocol.dialog.body.DialogBody;
import net.momirealms.craftengine.core.plugin.network.protocol.dialog.input.DialogInputControl;
import net.momirealms.craftengine.core.plugin.network.protocol.recipe.legacy.LegacyRecipe;
import net.momirealms.craftengine.core.plugin.network.protocol.recipe.modern.display.RecipeDisplay;
import net.momirealms.craftengine.core.plugin.network.protocol.recipe.modern.display.slot.SlotDisplay;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.function.Supplier;

public final class BuiltInRegistries {
    public static final Registry<BlockDefinition> BLOCK = new BlockRegistry<>(Registries.BLOCK, 512);
    public static final Registry<BlockBehaviorType<? extends BlockBehavior>> BLOCK_BEHAVIOR_TYPE = createConstantBoundRegistry(Registries.BLOCK_BEHAVIOR_TYPE, 64);
    public static final Registry<BlockEntityTintSourceType<? extends BlockEntityTintSource>> BLOCK_ENTITY_TINT_SOURCE_TYPE = createConstantBoundRegistry(Registries.BLOCK_ENTITY_TINT_SOURCE_TYPE, 16);
    public static final Registry<ItemProcessorType<? extends ItemProcessor>> ITEM_PROCESSOR_TYPE = createConstantBoundRegistry(Registries.ITEM_PROCESSOR_TYPE, 64);
    public static final Registry<ItemBehaviorType<? extends ItemBehavior>> ITEM_BEHAVIOR_TYPE = createConstantBoundRegistry(Registries.ITEM_BEHAVIOR_TYPE, 64);
    public static final Registry<PropertyType<? extends Comparable<?>>> PROPERTY_TYPE = createConstantBoundRegistry(Registries.PROPERTY_TYPE, 16);
    public static final Registry<NumberProviderType<? extends NumberProvider>> NUMBER_PROVIDER_TYPE = createConstantBoundRegistry(Registries.NUMBER_PROVIDER_TYPE, 16);
    public static final Registry<TemplateArgumentType<? extends TemplateArgument>> TEMPLATE_ARGUMENT_TYPE = createConstantBoundRegistry(Registries.TEMPLATE_ARGUMENT_TYPE, 16);
    public static final Registry<ItemModelType<? extends ItemModel>> ITEM_MODEL_TYPE = createConstantBoundRegistry(Registries.ITEM_MODEL_TYPE, 16);
    public static final Registry<TintType<? extends Tint>> TINT_TYPE = createConstantBoundRegistry(Registries.TINT_TYPE, 16);
    public static final Registry<SpecialModelType<? extends SpecialModel>> SPECIAL_MODEL_TYPE = createConstantBoundRegistry(Registries.SPECIAL_MODEL_TYPE, 16);
    public static final Registry<RangeDispatchPropertyType<? extends RangeDispatchProperty>> RANGE_DISPATCH_PROPERTY_TYPE = createConstantBoundRegistry(Registries.RANGE_DISPATCH_PROPERTY_TYPE, 16);
    public static final Registry<ConditionPropertyType<? extends ConditionProperty>> CONDITION_PROPERTY_TYPE = createConstantBoundRegistry(Registries.CONDITION_PROPERTY_TYPE, 16);
    public static final Registry<SelectPropertyType<? extends SelectProperty>> SELECT_PROPERTY_TYPE = createConstantBoundRegistry(Registries.SELECT_PROPERTY_TYPE, 16);
    public static final Registry<RecipeSerializer<? extends Recipe>> RECIPE_SERIALIZER = createConstantBoundRegistry(Registries.RECIPE_SERIALIZER, 16);
    public static final Registry<FormulaType<? extends Formula>> FORMULA_TYPE = createConstantBoundRegistry(Registries.FORMULA_TYPE, 16);
    public static final Registry<PathMatcherType<? extends Condition<PathContext>>> PATH_MATCHER_TYPE = createConstantBoundRegistry(Registries.PATH_MATCHER_TYPE, 16);
    public static final Registry<ResolutionType<? extends Resolution>> RESOLUTION_TYPE = createConstantBoundRegistry(Registries.RESOLUTION_TYPE, 16);
    public static final Registry<ItemTransformDataProcessor.Type<? extends ItemTransformDataProcessor>> TRANSFORM_PROCESSOR_TYPE = createConstantBoundRegistry(Registries.TRANSFORM_PROCESSOR_TYPE, 16);
    public static final Registry<ResourcePackHostType<? extends ResourcePackHost>> RESOURCE_PACK_HOST_TYPE = createConstantBoundRegistry(Registries.RESOURCE_PACK_HOST_TYPE, 16);
    public static final Registry<CommonFunctionType<? extends Function<Context>>> COMMON_FUNCTION_TYPE = createConstantBoundRegistry(Registries.COMMON_FUNCTION_TYPE, 128);
    public static final Registry<CommonConditionType<? extends Condition<Context>>> COMMON_CONDITION_TYPE = createConstantBoundRegistry(Registries.COMMON_CONDITION_TYPE, 128);
    public static final Registry<EquipmentType<? extends Equipment>> EQUIPMENT_TYPE = createConstantBoundRegistry(Registries.EQUIPMENT_TYPE, 8);
    public static final Registry<SlotDisplay.Type<? extends SlotDisplay>> SLOT_DISPLAY_TYPE = createConstantBoundRegistry(Registries.SLOT_DISPLAY_TYPE, 16);
    public static final Registry<RecipeDisplay.Type<? extends RecipeDisplay>> RECIPE_DISPLAY_TYPE = createConstantBoundRegistry(Registries.RECIPE_DISPLAY_TYPE, 16);
    public static final Registry<LegacyRecipe.Type<? extends LegacyRecipe>> LEGACY_RECIPE_TYPE = createConstantBoundRegistry(Registries.LEGACY_RECIPE_TYPE, 16);
    public static final Registry<PostProcessorType<? extends PostProcessor>> RECIPE_POST_PROCESSOR_TYPE = createConstantBoundRegistry(Registries.RECIPE_POST_PROCESSOR_TYPE, 16);
    public static final Registry<ItemUpdaterType<? extends ItemUpdater>> ITEM_UPDATER_TYPE = createConstantBoundRegistry(Registries.ITEM_UPDATER_TYPE, 16);
    public static final Registry<NetworkCodec<FriendlyByteBuf, ? extends ModPacket>> MOD_PACKET = createConstantBoundRegistry(Registries.MOD_PACKET, 16);
    public static final Registry<BlockEntityElementConfigType<? extends BlockEntityElement>> BLOCK_ENTITY_ELEMENT_TYPE = createConstantBoundRegistry(Registries.BLOCK_ENTITY_ELEMENT_TYPE, 16);
    public static final Registry<CraftRemainderType<? extends CraftRemainder>> CRAFT_REMAINDER_TYPE = createConstantBoundRegistry(Registries.CRAFT_REMAINDER_TYPE, 16);
    public static final Registry<FurnitureElementConfigType<? extends FurnitureElement>> FURNITURE_ELEMENT_TYPE = createConstantBoundRegistry(Registries.FURNITURE_ELEMENT_TYPE, 16);
    public static final Registry<FurnitureTintSourceType<? extends FurnitureTintSource>> FURNITURE_TINT_SOURCE_TYPE = createConstantBoundRegistry(Registries.FURNITURE_TINT_SOURCE_TYPE, 16);
    public static final Registry<FurnitureHitboxConfigType<? extends FurnitureHitBox>> FURNITURE_HITBOX_TYPE = createConstantBoundRegistry(Registries.FURNITURE_HITBOX_TYPE, 16);
    public static final Registry<FurnitureBehaviorType<? extends FurnitureBehaviorTemplate>> FURNITURE_BEHAVIOR_TYPE = createConstantBoundRegistry(Registries.FURNITURE_BEHAVIOR_TYPE, 32);
    public static final Registry<FurnitureSettingsModifierType<? extends FurnitureSettingsModifier>> FURNITURE_SETTINGS_TYPE = createConstantBoundRegistry(Registries.FURNITURE_SETTINGS_TYPE, 16);
    public static final Registry<BlockSettingsModifierType<? extends BlockSettingsModifier>> BLOCK_SETTINGS_TYPE = createConstantBoundRegistry(Registries.BLOCK_SETTINGS_TYPE, 16);
    public static final Registry<ItemSettingsModifierType<? extends ItemSettingsModifier>> ITEM_SETTINGS_TYPE = createConstantBoundRegistry(Registries.ITEM_SETTINGS_TYPE, 16);
    public static final Registry<LootFunctionType<? extends LootFunction>> LOOT_FUNCTION_TYPE = createConstantBoundRegistry(Registries.LOOT_FUNCTION_TYPE, 32);
    public static final Registry<LootEntryContainerType<? extends LootEntryContainer>> LOOT_ENTRY_CONTAINER_TYPE = createConstantBoundRegistry(Registries.LOOT_ENTRY_CONTAINER_TYPE, 16);
    public static final Registry<Dialog.Type<? extends Dialog>> DIALOG_TYPE = createConstantBoundRegistry(Registries.DIALOG_TYPE, 16);
    public static final Registry<DialogBody.Type<? extends DialogBody>> DIALOG_BODY_TYPE = createConstantBoundRegistry(Registries.DIALOG_BODY_TYPE, 8);
    public static final Registry<DialogAction.Type<? extends DialogAction>> DIALOG_ACTION_TYPE = createConstantBoundRegistry(Registries.DIALOG_ACTION_TYPE, 8);
    public static final Registry<DialogInputControl.Type<? extends DialogInputControl>> DIALOG_INPUT_CONTROL_TYPE = createConstantBoundRegistry(Registries.DIALOG_INPUT_CONTROL_TYPE, 8);
    public static final Registry<ClickEvent.Type<? extends ClickEvent>> CLICK_EVENT_TYPE = createConstantBoundRegistry(Registries.CLICK_EVENT_TYPE, 16);

    // todo 修改
    public static final Registry<PlayerSelectorType<? extends Context>> PLAYER_SELECTOR_TYPE = createConstantBoundRegistry(Registries.PLAYER_SELECTOR_TYPE, 16);

    private BuiltInRegistries() {}

    private static <T> Registry<T> createConstantBoundRegistry(ResourceKey<? extends Registry<T>> key, int expectedSize) {
        return new ConstantBoundRegistry<>(key, expectedSize);
    }

    private static <T> Registry<T> createConstantBoundRegistry(ResourceKey<? extends Registry<T>> key, int expectedSize, Supplier<T> initializer) {
        return new ConstantBoundRegistry<>(key, expectedSize);
    }
}
