package net.momirealms.craftengine.bukkit.plugin.injector;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.item.DataComponentTypes;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemDefinition;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.item.ItemTags;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.item.component.value.FireworkExplosion;
import net.momirealms.craftengine.core.util.Color;
import net.momirealms.craftengine.core.util.DyeColor;
import net.momirealms.craftengine.core.util.Tristate;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.world.ContainerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.*;
import net.momirealms.craftengine.proxy.minecraft.world.item.crafting.CraftingInputProxy;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SpecialRecipeMethods {
    private SpecialRecipeMethods() {
    }

    private static int size(Object input) {
        return VersionHelper.isOrAbove1_21 ? CraftingInputProxy.INSTANCE.size(input) : ContainerProxy.INSTANCE.getContainerSize(input);
    }

    private static Object getItem(Object input, int slot) {
        return VersionHelper.isOrAbove1_21 ? CraftingInputProxy.INSTANCE.getItem(input, slot) : ContainerProxy.INSTANCE.getItem(input, slot);
    }

    public static boolean matchesFireworkFade(@Argument(0) Object input) {
        if (VersionHelper.isOrAbove1_21 && CraftingInputProxy.INSTANCE.ingredientCount(input) < 2) {
            return false;
        }
        boolean hasDye = false;
        boolean hasFireworkStar = false;
        int size = size(input);
        for (int i = 0; i < size; i++) {
            Object itemStack = getItem(input, i);
            if (ItemStackProxy.INSTANCE.isEmpty(itemStack)) {
                continue;
            }
            Item wrapped = BukkitItemManager.instance().wrap(itemStack);
            if (isFireworkDye(wrapped)) {
                hasDye = true;
            } else {
                if (!wrapped.id().equals(ItemKeys.FIREWORK_STAR)) {
                    return false;
                }
                if (hasFireworkStar) {
                    return false;
                }
                hasFireworkStar = true;
            }
        }
        return hasDye && hasFireworkStar;
    }

    @RuntimeType
    public static Object assembleFireworkFade(@Argument(0) Object input) {
        IntList colors = new IntArrayList();
        Item starItem = null;
        int size = size(input);
        for (int i = 0; i < size; i++) {
            Object itemStack = getItem(input, i);
            if (ItemStackProxy.INSTANCE.isEmpty(itemStack)) {
                continue;
            }
            Item wrapped = BukkitItemManager.instance().wrap(itemStack);
            if (isFireworkDye(wrapped)) {
                Color color = getFireworkColor(wrapped);
                if (color == null) {
                    return ItemStackProxy.EMPTY;
                }
                colors.add(color.color());
            } else if (wrapped.id().equals(ItemKeys.FIREWORK_STAR)) {
                starItem = wrapped.copyWithCount(1);
            }
        }
        if (starItem == null || colors.isEmpty()) {
            return ItemStackProxy.EMPTY;
        }
        FireworkExplosion explosion = starItem.fireworkExplosion().orElse(FireworkExplosion.DEFAULT);
        starItem.fireworkExplosion(explosion.withFadeColors(colors));
        return starItem.minecraftItem();
    }

    public static boolean matchesRepair(@Argument(0) Object input) {
        if (VersionHelper.isOrAbove1_21 && CraftingInputProxy.INSTANCE.ingredientCount(input) != 2) {
            return false;
        }

        Item item1 = null;
        Item item2 = null;
        int size = size(input);
        for (int i = 0; i < size; i++) {
            Object itemStack = getItem(input, i);
            if (ItemStackProxy.INSTANCE.isEmpty(itemStack)) {
                continue;
            }
            if (item1 == null) {
                item1 = BukkitItemManager.instance().wrap(itemStack);
            } else if (item2 == null) {
                item2 = BukkitItemManager.instance().wrap(itemStack);
            } else {
                return false;
            }
        }
        return item1 != null && item2 != null && canCombine(item1, item2);
    }

    private static boolean canCombine(Item input1, Item input2) {
        if (input1.count() != 1 || !isDamageableItem(input1)) {
            return false;
        }
        if (input2.count() != 1 || !isDamageableItem(input2)) {
            return false;
        }
        if (!input1.id().equals(input2.id())) {
            return false;
        }
        ItemDefinition customItem = input1.getDefinitionOrNull();
        return customItem == null || customItem.settings().repairable().craftingTable() != Tristate.FALSE;
    }

    private static boolean isDamageableItem(Item item) {
        if (VersionHelper.isOrAbove1_20_5) {
            return item.hasComponent(DataComponentTypes.MAX_DAMAGE) && item.hasComponent(DataComponentTypes.DAMAGE);
        } else {
            return ItemProxy.INSTANCE.canBeDepleted(ItemStackProxy.INSTANCE.getItem(item.minecraftItem()));
        }
    }

    public static boolean matchesDye(@Argument(0) Object input) {
        if (VersionHelper.isOrAbove1_21 && CraftingInputProxy.INSTANCE.ingredientCount(input) < 2) {
            return false;
        }
        int size = size(input);
        Item itemToDye = null;
        boolean hasDye = false;
        for (int i = 0; i < size; i++) {
            Object itemStack = getItem(input, i);
            if (ItemStackProxy.INSTANCE.isEmpty(itemStack)) {
                continue;
            }
            Item wrapped = BukkitItemManager.instance().wrap(itemStack);
            if (isDyeable(wrapped)) {
                if (itemToDye != null) {
                    return false;
                }
                itemToDye = wrapped;
            } else {
                if (!isDye(wrapped)) {
                    return false;
                }
                hasDye = true;
            }
        }
        return hasDye && itemToDye != null;
    }

    @RuntimeType
    public static Object assembleDye(@Argument(0) Object input) {
        List<Color> colors = new ArrayList<>();
        Item itemToDye = null;
        int size = size(input);
        for (int i = 0; i < size; i++) {
            Object itemStack = getItem(input, i);
            if (ItemStackProxy.INSTANCE.isEmpty(itemStack)) {
                continue;
            }
            Item wrapped = BukkitItemManager.instance().wrap(itemStack);
            if (isDyeable(wrapped)) {
                itemToDye = wrapped.copyWithCount(1);
            } else {
                Color dyeColor = getDyeColor(wrapped);
                if (dyeColor != null) {
                    colors.add(dyeColor);
                } else {
                    return ItemStackProxy.EMPTY;
                }
            }
        }
        if (itemToDye == null || itemToDye.isEmpty() || colors.isEmpty()) {
            return ItemStackProxy.EMPTY;
        }
        return itemToDye.applyDyedColors(colors).minecraftItem();
    }

    @Nullable
    private static Color getDyeColor(final Item dyeItem) {
        ItemDefinition itemDefinition = dyeItem.getDefinitionOrNull();
        Color color = itemDefinition == null ? null : itemDefinition.settings().dyeColor();
        return color != null ? color : getVanillaDyeColor(dyeItem);
    }

    @Nullable
    private static Color getFireworkColor(final Item dyeItem) {
        ItemDefinition itemDefinition = dyeItem.getDefinitionOrNull();
        Color color = itemDefinition == null ? null : itemDefinition.settings().fireworkColor();
        return color != null ? color : getVanillaFireworkColor(dyeItem);
    }

    private static boolean isDyeable(final Item item) {
        ItemDefinition itemDefinition = item.getDefinitionOrNull();
        if (itemDefinition != null) {
            if (itemDefinition.settings().dyeable() == Tristate.FALSE) {
                return false;
            }
            if (itemDefinition.settings().dyeable() == Tristate.TRUE) {
                return true;
            }
        }
        if (VersionHelper.isOrAbove1_20_5) {
            return item.hasVanillaTag(ItemTags.DYEABLE);
        }
        return DyeableLeatherItemProxy.CLASS.isInstance(ItemStackProxy.INSTANCE.getItem(item.minecraftItem()));
    }

    @Nullable
    private static Color getVanillaDyeColor(final Item item) {
        Object itemStack = item.minecraftItem();
        Object dyeItem = ItemStackProxy.INSTANCE.getItem(itemStack);
        if (!DyeItemProxy.CLASS.isInstance(dyeItem)) {
            return null;
        }
        Object dyeColor = DyeItemProxy.INSTANCE.getDyeColor(dyeItem);
        int textureDiffuseColor;
        if (VersionHelper.isOrAbove1_21) {
            textureDiffuseColor = DyeColorProxy.INSTANCE.getTextureDiffuseColor(dyeColor);
        } else {
            float[] rgb = DyeColorProxy.INSTANCE.getTextureDiffuseColors(dyeColor);
            int r = (int) (rgb[0] * 255.0F);
            int g = (int) (rgb[1] * 255.0F);
            int b = (int) (rgb[2] * 255.0F);
            textureDiffuseColor = (r << 16) | (g << 8) | b;
        }
        return Color.fromDecimal(textureDiffuseColor);
    }

    @Nullable
    private static Color getVanillaFireworkColor(final Item item) {
        if (VersionHelper.isOrAbove26_1) {
            String colorType = (String) item.getComponentAsJava(DataComponentKeys.DYE);
            if (colorType == null) {
                return null;
            }
            return new Color(DyeColor.valueOf(colorType.toUpperCase(Locale.ROOT)).fireworkColor());
        } else {
            Object itemStack = item.minecraftItem();
            Object dyeItem = ItemStackProxy.INSTANCE.getItem(itemStack);
            if (!DyeItemProxy.CLASS.isInstance(dyeItem)) {
                return null;
            }
            return Color.fromDecimal(DyeColorProxy.INSTANCE.getFireworkColor(DyeItemProxy.INSTANCE.getDyeColor(dyeItem)));
        }
    }

    private static boolean isDye(Item dyeItem) {
        ItemDefinition itemDefinition = dyeItem.getDefinitionOrNull();
        if (itemDefinition != null) {
            return itemDefinition.settings().dyeColor() != null || isVanillaDyeItem(dyeItem);
        }
        return isVanillaDyeItem(dyeItem);
    }

    private static boolean isFireworkDye(Item dyeItem) {
        ItemDefinition itemDefinition = dyeItem.getDefinitionOrNull();
        if (itemDefinition != null) {
            return itemDefinition.settings().fireworkColor() != null || isVanillaDyeItem(dyeItem);
        }
        return isVanillaDyeItem(dyeItem);
    }

    private static boolean isVanillaDyeItem(Item item) {
        return DyeItemProxy.CLASS.isInstance(ItemStackProxy.INSTANCE.getItem(item.minecraftItem()));
    }
}
