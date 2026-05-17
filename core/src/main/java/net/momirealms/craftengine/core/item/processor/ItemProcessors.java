package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.processor.lore.*;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.*;

import java.util.Optional;
import java.util.function.Consumer;

public final class ItemProcessors {
    private ItemProcessors() {}

    public static final ItemProcessorType<ItemModelProcessor> ITEM_MODEL = register(Key.ce("item_model"), ItemModelProcessor.FACTORY, VersionHelper.isOrAbove1_21_2);
    public static final ItemProcessorType<SetArgumentsProcessor> ARGUMENTS = register(Key.ce("arguments"), SetArgumentsProcessor.FACTORY);
    public static final ItemProcessorType<SetArgumentsProcessor> SET_ARGUMENTS = register(Key.ce("set_arguments"), SetArgumentsProcessor.FACTORY);
    public static final ItemProcessorType<GetArgumentsProcessor> GET_ARGUMENTS = register(Key.ce("get_arguments"), GetArgumentsProcessor.FACTORY);
    public static final ItemProcessorType<OverwritableItemModelProcessor> OVERWRITABLE_ITEM_MODEL = register(Key.ce("overwritable_item_model"), OverwritableItemModelProcessor.FACTORY, VersionHelper.isOrAbove1_21_2);
    public static final ItemProcessorType<IdProcessor> ID = register(Key.ce("id"), IdProcessor.FACTORY);
    public static final ItemProcessorType<HideTooltipProcessor> HIDE_TOOLTIP = register(Key.ce("hide_tooltip"), HideTooltipProcessor.FACTORY);
    public static final ItemProcessorType<FoodProcessor> FOOD = register(Key.ce("food"), FoodProcessor.FACTORY, VersionHelper.isOrAbove1_20_5);
    public static final ItemProcessorType<ExternalSourceProcessor> EXTERNAL = register(Key.ce("external"), ExternalSourceProcessor.FACTORY);
    public static final ItemProcessorType<EquippableProcessor> EQUIPPABLE = register(Key.ce("equippable"), EquippableProcessor.FACTORY, VersionHelper.isOrAbove1_21_2);
    public static final ItemProcessorType<OverwritableEquippableAssetIdProcessor> EQUIPPABLE_ASSET_ID = register(Key.ce("overwritable_equippable_asset_id"), OverwritableEquippableAssetIdProcessor.FACTORY, VersionHelper.isOrAbove1_21_2);
    public static final ItemProcessorType<EnchantmentsProcessor> ENCHANTMENTS = register(Key.ce("enchantments"), EnchantmentsProcessor.FACTORY);
    public static final ItemProcessorType<EnchantmentsProcessor> ENCHANTMENT = register(Key.ce("enchantment"), EnchantmentsProcessor.FACTORY);
    public static final ItemProcessorType<DyedColorProcessor> DYED_COLOR = register(Key.ce("dyed_color"), DyedColorProcessor.FACTORY);
    public static final ItemProcessorType<ItemNameProcessor> DISPLAY_NAME = register(Key.ce("display_name"), ItemNameProcessor.FACTORY);
    public static final ItemProcessorType<ItemNameProcessor> ITEM_NAME = register(Key.ce("item_name"), ItemNameProcessor.FACTORY);
    public static final ItemProcessorType<CustomNameProcessor> CUSTOM_NAME = register(Key.ce("custom_name"), CustomNameProcessor.FACTORY);
    public static final ItemProcessorType<CustomModelDataProcessor> CUSTOM_MODEL_DATA = register(Key.ce("custom_model_data"), CustomModelDataProcessor.FACTORY);
    public static final ItemProcessorType<OverwritableCustomModelDataProcessor> OVERWRITABLE_CUSTOM_MODEL_DATA = register(Key.ce("overwritable_custom_model_data"), OverwritableCustomModelDataProcessor.FACTORY);
    public static final ItemProcessorType<ComponentsProcessor> COMPONENTS = register(Key.ce("components"), ComponentsProcessor.FACTORY, VersionHelper.isOrAbove1_20_5);
    public static final ItemProcessorType<ComponentsProcessor> COMPONENT = register(Key.ce("component"), ComponentsProcessor.FACTORY, VersionHelper.isOrAbove1_20_5);
    public static final ItemProcessorType<AttributeModifiersProcessor> ATTRIBUTE_MODIFIERS = register(Key.ce("attribute_modifiers"), AttributeModifiersProcessor.FACTORY);
    public static final ItemProcessorType<AttributeModifiersProcessor> ATTRIBUTES = register(Key.ce("attributes"), AttributeModifiersProcessor.FACTORY);
    public static final ItemProcessorType<PDCProcessor> PDC = register(Key.ce("pdc"), PDCProcessor.FACTORY);
    public static final ItemProcessorType<OverwritableItemNameProcessor> OVERWRITABLE_ITEM_NAME = register(Key.ce("overwritable_item_name"), OverwritableItemNameProcessor.FACTORY);
    public static final ItemProcessorType<JukeboxSongProcessor> JUKEBOX_PLAYABLE = register(Key.ce("jukebox_playable"), JukeboxSongProcessor.FACTORY, VersionHelper.isOrAbove1_21);
    public static final ItemProcessorType<RemoveComponentProcessor> REMOVE_COMPONENTS = register(Key.ce("remove_components"), RemoveComponentProcessor.FACTORY, VersionHelper.isOrAbove1_20_5);
    public static final ItemProcessorType<RemoveComponentProcessor> REMOVE_COMPONENT = register(Key.ce("remove_component"), RemoveComponentProcessor.FACTORY, VersionHelper.isOrAbove1_20_5);
    public static final ItemProcessorType<TagsProcessor> TAGS = register(Key.ce("tags"), TagsProcessor.FACTORY);
    public static final ItemProcessorType<TagsProcessor> NBT = register(Key.ce("nbt"), TagsProcessor.FACTORY);
    public static final ItemProcessorType<TooltipStyleProcessor> TOOLTIP_STYLE = register(Key.ce("tooltip_style"), TooltipStyleProcessor.FACTORY, VersionHelper.isOrAbove1_21_2);
    public static final ItemProcessorType<TrimProcessor> TRIM = register(Key.ce("trim"), TrimProcessor.FACTORY);
    public static final ItemProcessorType<LoreProcessor> LORE = register(Key.ce("lore"), LoreProcessor.FACTORY);
    public static final ItemProcessorType<UnbreakableProcessor> UNBREAKABLE = register(Key.ce("unbreakable"), UnbreakableProcessor.FACTORY);
    public static final ItemProcessorType<DynamicLoreProcessor> DYNAMIC_LORE = register(Key.ce("dynamic_lore"), DynamicLoreProcessor.FACTORY);
    public static final ItemProcessorType<InsertLoreProcessor> INSERT_LORE = register(Key.ce("insert_lore"), InsertLoreProcessor.FACTORY);
    public static final ItemProcessorType<RemoveLoreProcessor> REMOVE_LORE = register(Key.ce("remove_lore"), RemoveLoreProcessor.FACTORY);
    public static final ItemProcessorType<OverwritableLoreProcessor> OVERWRITABLE_LORE = register(Key.ce("overwritable_lore"), OverwritableLoreProcessor.FACTORY);
    public static final ItemProcessorType<MaxDamageProcessor> MAX_DAMAGE = register(Key.ce("max_damage"), MaxDamageProcessor.FACTORY, VersionHelper.isOrAbove1_20_5);
    public static final ItemProcessorType<BlockStateProcessor> BLOCK_STATE = register(Key.ce("block_state"), BlockStateProcessor.FACTORY);
    public static final ItemProcessorType<BlockStateProcessor> BLOCKSTATE = register(Key.ce("blockstate"), BlockStateProcessor.FACTORY);
    public static final ItemProcessorType<ConditionalProcessor> CONDITIONAL = register(Key.ce("conditional"), ConditionalProcessor.FACTORY, VersionHelper.PREMIUM);
    public static final ItemProcessorType<ConditionalProcessor> CONDITION = register(Key.ce("condition"), ConditionalProcessor.FACTORY, VersionHelper.PREMIUM);
    public static final ItemProcessorType<ProfileProcessor> PROFILE = register(Key.ce("profile"), ProfileProcessor.FACTORY);
    public static final ItemProcessorType<OverwritableDyedColorProcessor> OVERWRITABLE_DYED_COLOR = register(Key.ce("overwritable_dyed_color"), OverwritableDyedColorProcessor.FACTORY);
    public static final ItemProcessorType<UseRemainderProcessor> USE_REMAINDER = register(Key.ce("use_remainder"), UseRemainderProcessor.FACTORY, VersionHelper.isOrAbove1_21_2);
    public static final ItemProcessorType<WrittenBookTagsProcessor> PROCESS_WRITTEN_BOOK_TAGS = register(Key.ce("process_written_book_tags"), WrittenBookTagsProcessor.FACTORY);
    public static final ItemProcessorType<PaintingVariantProcessor> PAINTING_VARIANT = register(Key.ce("painting_variant"), PaintingVariantProcessor.FACTORY);

    public static <T extends ItemProcessor> ItemProcessorType<T> register(Key key, ItemProcessorFactory<T> factory) {
        ItemProcessorType<T> type = new ItemProcessorType<>(key, factory);
        ((WritableRegistry<ItemProcessorType<?>>) BuiltInRegistries.ITEM_PROCESSOR_TYPE)
                .register(ResourceKey.create(Registries.ITEM_PROCESSOR_TYPE.location(), key), type);
        return type;
    }

    private static <T extends ItemProcessor> ItemProcessorType<T> register(Key key, ItemProcessorFactory<T> factory, boolean condition) {
        return register(key, condition ? factory : null);
    }

    public static void collectProcessors(ConfigSection dataSection, Consumer<ItemProcessor> callback) {
        ExceptionCollector<KnownResourceException> errorCollector = new ExceptionCollector<>(KnownResourceException.class);
        if (dataSection != null) {
            for (String type : dataSection.keySet()) {
                ConfigValue value = dataSection.getValue(type);
                if (value == null) continue;
                String key = StringUtils.normalizeSettingsType(type);
                errorCollector.runCatching(() -> {
                    Optional.ofNullable(BuiltInRegistries.ITEM_PROCESSOR_TYPE.getValue(Key.ce(key))).ifPresent(processorType -> {
                        ItemProcessorFactory<? extends ItemProcessor> factory = processorType.factory();
                        if (factory != null) {
                            callback.accept(factory.create(value));
                        }
                    });
                });
            }
        }
        errorCollector.throwIfPresent();
    }

    public static void init() {}
}
