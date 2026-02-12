package net.momirealms.craftengine.bukkit.util;

import com.mojang.serialization.Dynamic;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.bukkit.CraftBukkitReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MReferences;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MRegistryOps;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.recipe.UniqueIdItem;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.sparrow.nbt.Tag;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public final class ItemStackUtils {

    private ItemStackUtils() {}

    @Contract("null -> true")
    public static boolean isEmpty(final ItemStack item) {
        if (item == null) return true;
        if (item.getType() == Material.AIR) return true;
        return item.getAmount() == 0;
    }

    public static boolean hasCustomItem(ItemStack[] stack) {
        for (ItemStack itemStack : stack) {
            if (!ItemStackUtils.isEmpty(itemStack)) {
                if (BukkitItemManager.instance().wrap(itemStack).customId().isPresent()) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isCustomItem(ItemStack stack) {
        if (!ItemStackUtils.isEmpty(stack)) {
            return BukkitItemManager.instance().wrap(stack).customId().isPresent();
        }
        return false;
    }

    public static ItemStack ensureCraftItemStack(ItemStack itemStack) {
        if (CraftBukkitReflections.clazz$CraftItemStack.isInstance(itemStack)) {
            return itemStack;
        } else {
            return FastNMS.INSTANCE.method$CraftItemStack$asCraftCopy(itemStack);
        }
    }

    public static UniqueIdItem<ItemStack> getUniqueIdItem(@Nullable ItemStack itemStack) {
        Item<ItemStack> wrappedItem = BukkitItemManager.instance().wrap(itemStack);
        return UniqueIdItem.of(wrappedItem);
    }

    public static ItemStack asCraftMirror(Object itemStack) {
        return FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(itemStack);
    }

    @SuppressWarnings("DataFlowIssue")
    @Nullable
    public static Tag saveNMSItemStackAsTag(Object nmsStack) {
        if (VersionHelper.COMPONENT_RELEASE) {
            return CoreReflections.instance$ItemStack$CODEC.encodeStart(MRegistryOps.SPARROW_NBT, nmsStack)
                    .resultOrPartial(error -> CraftEngine.instance().logger().severe("Error while saving item: " + error))
                    .orElse(null);
        } else {
            Object nmsTag = FastNMS.INSTANCE.method$itemStack$save(nmsStack, FastNMS.INSTANCE.constructor$CompoundTag());
            return MRegistryOps.NBT.convertTo(MRegistryOps.SPARROW_NBT, nmsTag);
        }
    }

    @Nullable
    public static Tag saveItemStackAsTag(ItemStack itemStack) {
        return saveNMSItemStackAsTag(FastNMS.INSTANCE.field$CraftItemStack$handle(ensureCraftItemStack(itemStack)));
    }

    @SuppressWarnings("DataFlowIssue")
    @Nullable
    public static Object parseNMSItemStack(Tag tag, int dataVersion) {
        Tag itemTag = tag;
        int currentVersion = VersionHelper.WORLD_VERSION;
        if (Config.enableItemDataFixerUpper() && dataVersion != currentVersion) {
            Dynamic<Tag> input = new Dynamic<>(MRegistryOps.SPARROW_NBT, itemTag);
            itemTag = CoreReflections.instance$DataFixer.update(MReferences.ITEM_STACK, input, dataVersion, currentVersion).getValue();
        }
        final Tag finalItemTag = itemTag;
        if (VersionHelper.COMPONENT_RELEASE) {
            return CoreReflections.instance$ItemStack$CODEC.parse(MRegistryOps.SPARROW_NBT, finalItemTag)
                    .resultOrPartial(error -> CraftEngine.instance().logger().severe("Tried to load invalid item: '" + finalItemTag + "'. " + error))
                    .orElse(null);
        } else {
            Object nmsTag = MRegistryOps.SPARROW_NBT.convertTo(MRegistryOps.NBT, finalItemTag);
            return FastNMS.INSTANCE.method$ItemStack$of(nmsTag);
        }
    }

    @Nullable
    public static ItemStack parseItemStack(Tag tag, int dataVersion) {
        return asCraftMirror(parseNMSItemStack(tag, dataVersion));
    }
}
