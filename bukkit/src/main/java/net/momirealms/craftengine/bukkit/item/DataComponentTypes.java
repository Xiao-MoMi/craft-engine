package net.momirealms.craftengine.bukkit.item;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBuiltInRegistries;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.item.DataComponentKeys;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;

public final class DataComponentTypes {
    public static final Object CUSTOM_MODEL_DATA = byId(DataComponentKeys.CUSTOM_MODEL_DATA);
    public static final Object CUSTOM_NAME = byId(DataComponentKeys.CUSTOM_NAME);
    public static final Object ITEM_NAME = byId(DataComponentKeys.ITEM_NAME);
    public static final Object LORE = byId(DataComponentKeys.LORE);
    public static final Object DAMAGE = byId(DataComponentKeys.DAMAGE);
    public static final Object MAX_DAMAGE = byId(DataComponentKeys.MAX_DAMAGE);
    public static final Object ENCHANTMENT_GLINT_OVERRIDE = byId(DataComponentKeys.ENCHANTMENT_GLINT_OVERRIDE);
    public static final Object ENCHANTMENTS = byId(DataComponentKeys.ENCHANTMENTS);
    public static final Object STORED_ENCHANTMENTS = byId(DataComponentKeys.STORED_ENCHANTMENTS);
    public static final Object UNBREAKABLE = byId(DataComponentKeys.UNBREAKABLE);
    public static final Object MAX_STACK_SIZE = byId(DataComponentKeys.MAX_STACK_SIZE);
    public static final Object EQUIPPABLE = byId(DataComponentKeys.EQUIPPABLE);
    public static final Object ITEM_MODEL = byId(DataComponentKeys.ITEM_MODEL);
    public static final Object TOOLTIP_STYLE = byId(DataComponentKeys.TOOLTIP_STYLE);
    public static final Object JUKEBOX_PLAYABLE = byId(DataComponentKeys.JUKEBOX_PLAYABLE);
    public static final Object TRIM = byId(DataComponentKeys.TRIM);
    public static final Object REPAIR_COST = byId(DataComponentKeys.REPAIR_COST);
    public static final Object CUSTOM_DATA = byId(DataComponentKeys.CUSTOM_DATA);
    public static final Object PROFILE = byId(DataComponentKeys.PROFILE);
    public static final Object DYED_COLOR = byId(DataComponentKeys.DYED_COLOR);
    public static final Object DEATH_PROTECTION = byId(DataComponentKeys.DEATH_PROTECTION);
    public static final Object FIREWORK_EXPLOSION = byId(DataComponentKeys.FIREWORK_EXPLOSION);
    public static final Object BUNDLE_CONTENTS = byId(DataComponentKeys.BUNDLE_CONTENTS);
    public static final Object CONTAINER = byId(DataComponentKeys.CONTAINER);
    public static final Object BLOCK_STATE = byId(DataComponentKeys.BLOCK_STATE);

    private DataComponentTypes() {}

    public static Object byId(Key key) {
        if (!VersionHelper.isOrAbove1_20_5()) return null;
        return FastNMS.INSTANCE.method$Registry$getValue(MBuiltInRegistries.DATA_COMPONENT_TYPE, KeyUtils.toIdentifier(key));
    }
}
