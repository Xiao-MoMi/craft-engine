package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.item.ItemDataModifierFactory;
import net.momirealms.craftengine.core.item.modifier.lore.OverwritableLoreModifier;
import net.momirealms.craftengine.core.item.modifier.lore.DynamicLoreModifier;
import net.momirealms.craftengine.core.item.modifier.lore.LoreModifier;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;
import net.momirealms.craftengine.core.util.VersionHelper;

public final class ItemDataModifiers {
    private ItemDataModifiers() {}

    public static final Key ITEM_MODEL = Key.of("craftengine:item-model");
    public static final Key ID = Key.of("craftengine:id");
    public static final Key HIDE_TOOLTIP = Key.of("craftengine:hide-tooltip");
    public static final Key FOOD = Key.of("craftengine:food");
    public static final Key EXTERNAL = Key.of("craftengine:external");
    public static final Key EQUIPPABLE = Key.of("craftengine:equippable");
    public static final Key EQUIPPABLE_ASSET_ID = Key.of("craftengine:equippable-asset-id");
    public static final Key ENCHANTMENT = Key.of("craftengine:enchantment");
    public static final Key ENCHANTMENTS = Key.of("craftengine:enchantments");
    public static final Key DYED_COLOR = Key.of("craftengine:dyed-color");
    public static final Key DISPLAY_NAME = Key.of("craftengine:display-name");
    public static final Key CUSTOM_NAME = Key.of("craftengine:custom-name");
    public static final Key CUSTOM_MODEL_DATA = Key.of("craftengine:custom-model-data");
    public static final Key COMPONENTS = Key.of("craftengine:components");
    public static final Key ATTRIBUTE_MODIFIERS = Key.of("craftengine:attribute-modifiers");
    public static final Key ATTRIBUTES = Key.of("craftengine:attributes");
    public static final Key ARGUMENTS = Key.of("craftengine:arguments");
    public static final Key ITEM_NAME = Key.of("craftengine:item-name");
    public static final Key OVERWRITABLE_ITEM_NAME = Key.of("craftengine:overwritable-item-name");
    public static final Key JUKEBOX_PLAYABLE = Key.of("craftengine:jukebox-playable");
    public static final Key REMOVE_COMPONENTS = Key.of("craftengine:remove-components");
    public static final Key TAGS = Key.of("craftengine:tags");
    public static final Key NBT = Key.of("craftengine:nbt");
    public static final Key TOOLTIP_STYLE = Key.of("craftengine:tooltip-style");
    public static final Key TRIM = Key.of("craftengine:trim");
    public static final Key LORE = Key.of("craftengine:lore");
    public static final Key UNBREAKABLE = Key.of("craftengine:unbreakable");
    public static final Key DYNAMIC_LORE = Key.of("craftengine:dynamic-lore");
    public static final Key OVERWRITABLE_LORE = Key.of("craftengine:overwritable-lore");

    public static <T> void register(Key key, ItemDataModifierFactory<T> factory) {
        ((WritableRegistry<ItemDataModifierFactory<?>>) BuiltInRegistries.ITEM_DATA_MODIFIER_FACTORY)
                .register(ResourceKey.create(Registries.ITEM_DATA_MODIFIER_FACTORY.location(), key), factory);
        if (key.value().contains("-")) {
            ((WritableRegistry<ItemDataModifierFactory<?>>) BuiltInRegistries.ITEM_DATA_MODIFIER_FACTORY)
                    .register(ResourceKey.create(Registries.ITEM_DATA_MODIFIER_FACTORY.location(), new Key(key.namespace(), key.value().replace("-", "_"))), factory);
        }
    }

    public static void init() {}

    static {
        register(EXTERNAL, ExternalModifier.FACTORY);
        register(LORE, LoreModifier.FACTORY);
        register(DYNAMIC_LORE, DynamicLoreModifier.FACTORY);
        register(OVERWRITABLE_LORE, OverwritableLoreModifier.FACTORY);
        register(DYED_COLOR, DyedColorModifier.FACTORY);
        register(TAGS, TagsModifier.FACTORY);
        register(NBT, TagsModifier.FACTORY);
        register(ATTRIBUTE_MODIFIERS, AttributeModifiersModifier.FACTORY);
        register(ATTRIBUTES, AttributeModifiersModifier.FACTORY);
        register(CUSTOM_MODEL_DATA, CustomModelDataModifier.FACTORY);
        register(UNBREAKABLE, UnbreakableModifier.FACTORY);
        register(ENCHANTMENT, EnchantmentsModifier.FACTORY);
        register(ENCHANTMENTS, EnchantmentsModifier.FACTORY);
        register(TRIM, TrimModifier.FACTORY);
        register(HIDE_TOOLTIP, HideTooltipModifier.FACTORY);
        register(ARGUMENTS, ArgumentsModifier.FACTORY);
        register(OVERWRITABLE_ITEM_NAME, OverwritableItemNameModifier.FACTORY);
        if (VersionHelper.isOrAbove1_20_5()) {
            register(CUSTOM_NAME, CustomNameModifier.FACTORY);
            register(ITEM_NAME, ItemNameModifier.FACTORY);
            register(DISPLAY_NAME, ItemNameModifier.FACTORY);
            register(COMPONENTS, ComponentsModifier.FACTORY);
            register(REMOVE_COMPONENTS, RemoveComponentModifier.FACTORY);
            register(FOOD, FoodModifier.FACTORY);
        } else {
            register(CUSTOM_NAME, CustomNameModifier.FACTORY);
            register(ITEM_NAME, CustomNameModifier.FACTORY);
            register(DISPLAY_NAME, CustomNameModifier.FACTORY);
        }
        if (VersionHelper.isOrAbove1_21()) {
            register(JUKEBOX_PLAYABLE, JukeboxSongModifier.FACTORY);
        }
        if (VersionHelper.isOrAbove1_21_2()) {
            register(TOOLTIP_STYLE, TooltipStyleModifier.FACTORY);
            register(ITEM_MODEL, ItemModelModifier.FACTORY);
            register(EQUIPPABLE, EquippableModifier.FACTORY);
        }
    }
}
