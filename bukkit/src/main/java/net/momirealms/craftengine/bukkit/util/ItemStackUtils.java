package net.momirealms.craftengine.bukkit.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import com.mojang.serialization.Dynamic;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.item.BukkitItem;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemTags;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.item.recipe.UniqueIdItem;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.inventory.CraftItemStackProxy;
import net.momirealms.craftengine.proxy.minecraft.nbt.CompoundTagProxy;
import net.momirealms.craftengine.proxy.minecraft.util.DataFixersProxy;
import net.momirealms.craftengine.proxy.minecraft.util.datafix.fixes.ReferencesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.LivingEntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackTemplateProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.component.ToolProxy;
import net.momirealms.craftengine.proxy.spottedleaf.dataconverter.minecraft.MCDataConverterProxy;
import net.momirealms.craftengine.proxy.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistryProxy;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.ListTag;
import net.momirealms.sparrow.nbt.NBT;
import net.momirealms.sparrow.nbt.Tag;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public final class ItemStackUtils {
    private static final Cache<VersionedItemTag, Object> ITEM_PARSER_CACHE = Caffeine.newBuilder()
            .maximumSize(8192)
            .expireAfterAccess(Duration.of(15, ChronoUnit.MINUTES))
            .scheduler(Scheduler.systemScheduler())
            .executor(CraftEngine.instance().scheduler().async())
            .build();

    private ItemStackUtils() {}

    @Contract("null -> true")
    public static boolean isEmpty(final ItemStack item) {
        if (item == null) return true;
        if (VersionHelper.hasPaperPatch) {
            return item.isEmpty();
        }
        if (item.getType() == Material.AIR) return true;
        return item.getAmount() <= 0;
    }

    public static BukkitItem wrap(final Object itemStack) {
        return BukkitItemManager.instance().wrap(itemStack);
    }

    public static boolean hasCustomItem(ItemStack[] stack) {
        for (ItemStack itemStack : stack) {
            if (!ItemStackUtils.isEmpty(itemStack)) {
                if (BukkitAdaptor.adapt(itemStack).customId().isPresent()) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isCustomItem(ItemStack stack) {
        if (!ItemStackUtils.isEmpty(stack)) {
            return BukkitAdaptor.adapt(stack).customId().isPresent();
        }
        return false;
    }

    public static ItemStack fromBytes(byte[] bytes) {
        CompoundTag compoundTag = NBT.readCompressed(bytes, false);
        if (compoundTag.isEmpty()) {
            compoundTag = NBT.readCompressed(bytes, true);
        }
        return parseBukkitItem(compoundTag, compoundTag.getInt("DataVersion", Config.itemDataFixerUpperFallbackVersion()));
    }

    public static byte[] toBytes(ItemStack itemStack) {
        CompoundTag tag = saveBukkitItemAsTag(itemStack);
        if (tag == null) {
            throw new IllegalStateException("Could not save item: " + itemStack);
        }
        tag.putInt("DataVersion", VersionHelper.WORLD_VERSION);
        return NBT.writeCompressed(tag, false);
    }

    public static ItemStack ensureCraftItemStack(ItemStack itemStack) {
        if (CraftItemStackProxy.CLASS.isInstance(itemStack)) {
            return itemStack;
        } else {
            return CraftItemStackProxy.INSTANCE.asCraftCopy(itemStack);
        }
    }

    public static UniqueIdItem getUniqueIdItem(@Nullable ItemStack itemStack) {
        return UniqueIdItem.of(BukkitItemManager.instance().wrap(itemStack));
    }

    public static ItemStack asCraftMirror(Object itemStack) {
        return CraftItemStackProxy.INSTANCE.asCraftMirror(itemStack);
    }

    public static ItemStack getBukkitStack(Object itemStack) {
        if (VersionHelper.hasPaperPatch) {
            return ItemStackProxy.INSTANCE.getBukkitStack(itemStack);
        } else {
            return asCraftMirror(itemStack);
        }
    }

    public static List<ItemStack> getBukkitStacks(List<Item> items) {
        List<ItemStack> result = new ArrayList<>(items.size());
        for (Item item : items) {
            result.add((ItemStack) item.platformItem());
        }
        return result;
    }

    public static ItemStack getBukkitStack(Item item) {
        return getBukkitStack(item.minecraftItem());
    }

    public static Object unwrap(ItemStack itemStack) {
        ItemStack craftItemStack = ensureCraftItemStack(itemStack);
        Object handle = CraftItemStackProxy.INSTANCE.getHandle(craftItemStack);
        return handle == null ? ItemStackProxy.EMPTY : handle;
    }

    @Nullable
    public static Tag saveMinecraftItemStackAsTag(Object nmsStack) {
        if (VersionHelper.COMPONENT_RELEASE) {
            return ItemStackProxy.INSTANCE.getCodec().encodeStart(RegistryOps.SPARROW_NBT, nmsStack)
                    .resultOrPartial(error -> CraftEngine.instance().logger().error("Error while saving item: " + error))
                    .orElse(null);
        } else {
            Object nmsTag = ItemStackProxy.INSTANCE.save(nmsStack, CompoundTagProxy.INSTANCE.newInstance());
            return RegistryOps.NBT.convertTo(RegistryOps.SPARROW_NBT, nmsTag);
        }
    }

    @Nullable
    public static CompoundTag saveBukkitItemAsTag(ItemStack itemStack) {
        return (CompoundTag) saveMinecraftItemStackAsTag(ItemStackUtils.unwrap(ensureCraftItemStack(itemStack)));
    }

    @Nullable
    public static Object parseCachedMinecraftItem(Tag tag, int dataVersion) {
        Object item = ITEM_PARSER_CACHE.get(new VersionedItemTag(tag, dataVersion), k -> parseMinecraftItem(k.tag, k.dataVersion));
        if (item == null) {
            return null;
        }
        return ItemStackProxy.INSTANCE.copy(item);
    }

    @Nullable
    public static Object parseMinecraftItem(Tag tag, int dataVersion) {
        Tag itemTag = tag;
        int currentVersion = VersionHelper.WORLD_VERSION;
        if (Config.enableItemDataFixerUpper() && dataVersion != currentVersion) {
            if (VersionHelper.hasPaperPatch && VersionHelper.MINECRAFT_VERSION != MinecraftVersion.V1_21_5) {
                Object nmsTag = RegistryOps.SPARROW_NBT.convertTo(RegistryOps.NBT, itemTag);
                Object converted = MCDataConverterProxy.INSTANCE.convertTag(MCTypeRegistryProxy.ITEM_STACK, nmsTag, dataVersion, currentVersion);
                // 升级结果已经是原版 NBT，直接解析，避免再构造一棵 Sparrow NBT 树。
                if (VersionHelper.COMPONENT_RELEASE) {
                    return ItemStackProxy.INSTANCE.getCodec().parse(RegistryOps.NBT, converted)
                            .resultOrPartial(error -> CraftEngine.instance().logger().error("Tried to load invalid item: '" + converted + "'. " + error))
                            .orElse(null);
                } else {
                    // 旧版本也直接使用原版 NBT，避免转回 Sparrow 后又转成原版 NBT。
                    return ItemStackProxy.INSTANCE.of(converted);
                }
            } else {
                Dynamic<Tag> input = new Dynamic<>(RegistryOps.SPARROW_NBT, itemTag);
                itemTag = DataFixersProxy.INSTANCE.getDataFixer().update(ReferencesProxy.ITEM_STACK, input, dataVersion, currentVersion).getValue();
            }
        }
        final Tag finalItemTag = itemTag;
        return parseMinecraftItem(finalItemTag);
    }

    @Nullable
    public static Object parseMinecraftItem(Tag tag) {
        if (VersionHelper.COMPONENT_RELEASE) {
            return ItemStackProxy.INSTANCE.getCodec().parse(RegistryOps.SPARROW_NBT, tag)
                    .resultOrPartial(error -> CraftEngine.instance().logger().error("Tried to load invalid item: '" + tag + "'. " + error))
                    .orElse(null);
        } else {
            Object nmsTag = RegistryOps.SPARROW_NBT.convertTo(RegistryOps.NBT, tag);
            return ItemStackProxy.INSTANCE.of(nmsTag);
        }
    }

    @Nullable
    public static ItemStack parseBukkitItem(Tag tag, int dataVersion) {
        return asCraftMirror(parseMinecraftItem(tag, dataVersion));
    }

    public static void hurtAndBreak(Object nmsStack, int amount, Object livingEntity, Object slot) {
        if (VersionHelper.isOrAbove1_20_5) {
            ItemStackProxy.INSTANCE.hurtAndBreak(nmsStack, amount, livingEntity, slot);
        } else {
            ItemStackProxy.INSTANCE.hurtAndBreak(nmsStack, amount, livingEntity, entity -> LivingEntityProxy.INSTANCE.broadcastBreakEvent(entity, slot));
        }
    }

    public static ItemStack[] parseBukkitItems(ListTag tag, int size, int dataVersion) {
        ItemStack[] itemStacks = new ItemStack[size];
        for (int i = 0; i < tag.size(); i++) {
            CompoundTag itemTag = tag.getCompound(i);
            int slot = itemTag.getInt("slot");
            if (slot < 0 || slot >= itemStacks.length) {
                continue;
            }
            itemStacks[slot] = ItemStackUtils.parseBukkitItem(itemTag, dataVersion);
        }
        return itemStacks;
    }

    public static ListTag saveBukkitItemsAsListTag(ItemStack[] itemStack) {
        ListTag itemsTag = new ListTag();
        for (int i = 0; i < itemStack.length; i++) {
            if (itemStack[i] == null || !(saveBukkitItemAsTag(itemStack[i]) instanceof CompoundTag itemTag)) {
                continue;
            }
            itemTag.putInt("slot", i);
            itemsTag.add(itemTag);
        }
        return itemsTag;
    }

    public static Object toItemStackTemplate(Item item) {
        Object minecraftItem = item.minecraftItem();
        return ItemStackTemplateProxy.INSTANCE.newInstance(
                ItemStackProxy.INSTANCE.typeHolder(minecraftItem),
                ItemStackProxy.INSTANCE.getCount(minecraftItem),
                ItemStackProxy.INSTANCE.getComponentsPatch(minecraftItem)
        );
    }

    public static boolean canBreakBlockInCreativeMode(BukkitItem item) {
        if (VersionHelper.isOrAbove1_21_5) {
            Object tool = item.getExactComponent(DataComponentKeys.TOOL);
            if (tool == null) {
                return true;
            }
            return ToolProxy.INSTANCE.canDestroyBlocksInCreative(tool);
        } else {
            Material material = item.getBukkitItem().getType();
            return material != Material.DEBUG_STICK
                    && material != Material.TRIDENT
                    && (!VersionHelper.isOrAbove1_20_5 || material != MaterialUtils.MACE)
                    && !item.hasVanillaTag(ItemTags.SWORDS);
        }
    }

    public static String getDescriptionId(ItemStack itemStack) {
        Object unwrap = unwrap(itemStack);
        Object item = ItemStackProxy.INSTANCE.getItem(unwrap);
        return ItemProxy.INSTANCE.getDescriptionId(item);
    }

    public static class VersionedItemTag {
        public final Tag tag;
        public final int dataVersion;

        public VersionedItemTag(Tag tag, int dataVersion) {
            this.tag = tag;
            this.dataVersion = dataVersion;
        }

        public Tag tag() {
            return this.tag;
        }

        public int dataVersion() {
            return this.dataVersion;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof VersionedItemTag versionedItemTag) {
                return this.tag.equals(versionedItemTag.tag) && this.dataVersion == versionedItemTag.dataVersion;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return this.tag.hashCode();
        }
    }
}
