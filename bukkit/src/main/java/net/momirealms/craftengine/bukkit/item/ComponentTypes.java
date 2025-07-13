package net.momirealms.craftengine.bukkit.item;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBuiltInRegistries;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.item.ComponentKeys;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;

public class ComponentTypes {
    public static final Object CUSTOM_MODEL_DATA = getComponentType(ComponentKeys.CUSTOM_MODEL_DATA);
    public static final Object CUSTOM_NAME = getComponentType(ComponentKeys.CUSTOM_NAME);
    public static final Object ITEM_NAME = getComponentType(ComponentKeys.ITEM_NAME);
    public static final Object LORE = getComponentType(ComponentKeys.LORE);
    public static final Object DAMAGE = getComponentType(ComponentKeys.DAMAGE);
    public static final Object MAX_DAMAGE = getComponentType(ComponentKeys.MAX_DAMAGE);
    public static final Object ENCHANTMENT_GLINT_OVERRIDE = getComponentType(ComponentKeys.ENCHANTMENT_GLINT_OVERRIDE);
    public static final Object ENCHANTMENTS = getComponentType(ComponentKeys.ENCHANTMENTS);
    public static final Object STORED_ENCHANTMENTS = getComponentType(ComponentKeys.STORED_ENCHANTMENTS);
    public static final Object UNBREAKABLE = getComponentType(ComponentKeys.UNBREAKABLE);
    public static final Object MAX_STACK_SIZE = getComponentType(ComponentKeys.MAX_STACK_SIZE);
    public static final Object EQUIPPABLE = getComponentType(ComponentKeys.EQUIPPABLE);
    public static final Object ITEM_MODEL = getComponentType(ComponentKeys.ITEM_MODEL);
    public static final Object TOOLTIP_STYLE = getComponentType(ComponentKeys.TOOLTIP_STYLE);
    public static final Object JUKEBOX_PLAYABLE = getComponentType(ComponentKeys.JUKEBOX_PLAYABLE);
    public static final Object TRIM = getComponentType(ComponentKeys.TRIM);
    public static final Object REPAIR_COST = getComponentType(ComponentKeys.REPAIR_COST);
    public static final Object CUSTOM_DATA = getComponentType(ComponentKeys.CUSTOM_DATA);
    public static final Object PROFILE = getComponentType(ComponentKeys.PROFILE);
    public static final Object DYED_COLOR = getComponentType(ComponentKeys.DYED_COLOR);
    public static final Object DEATH_PROTECTION = getComponentType(ComponentKeys.DEATH_PROTECTION);
    public static final Object FIREWORK_EXPLOSION = getComponentType(ComponentKeys.FIREWORK_EXPLOSION);

    private ComponentTypes() {}

    private static Object getComponentType(Key key) {
        if (!VersionHelper.isOrAbove1_20_5()) return null;
        return FastNMS.INSTANCE.method$Registry$getValue(MBuiltInRegistries.DATA_COMPONENT_TYPE, KeyUtils.toResourceLocation(key));
    }
}
