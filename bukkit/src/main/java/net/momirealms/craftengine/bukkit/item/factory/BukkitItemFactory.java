package net.momirealms.craftengine.bukkit.item.factory;

import com.google.gson.JsonElement;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBuiltInRegistries;
import net.momirealms.craftengine.bukkit.util.ItemTags;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.item.ExternalItemSource;
import net.momirealms.craftengine.core.item.ItemFactory;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.item.ItemWrapper;
import net.momirealms.craftengine.core.item.data.JukeboxPlayable;
import net.momirealms.craftengine.core.item.setting.EquipmentData;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.StringUtils;
import net.momirealms.craftengine.core.util.UniqueKey;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.sparrow.nbt.Tag;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.Optional;

public abstract class BukkitItemFactory<W extends ItemWrapper<ItemStack>> extends ItemFactory<W, ItemStack> {
    private boolean hasExternalRecipeSource = false;
    private ExternalItemSource<ItemStack>[] recipeIngredientSources = null;

    protected BukkitItemFactory(CraftEngine plugin) {
        super(plugin);
    }

    public static BukkitItemFactory<? extends ItemWrapper<ItemStack>> create(CraftEngine plugin) {
        Objects.requireNonNull(plugin, "plugin");
        if (VersionHelper.isOrAbove1_21_5()) {
            return new ComponentItemFactory1_21_5(plugin);
        } else if (VersionHelper.isOrAbove1_21_4()) {
            return new ComponentItemFactory1_21_4(plugin);
        } else if (VersionHelper.isOrAbove1_21_2()) {
            return new ComponentItemFactory1_21_2(plugin);
        } else if (VersionHelper.isOrAbove1_21()) {
            return new ComponentItemFactory1_21(plugin);
        } else if (VersionHelper.isOrAbove1_20_5()) {
            return new ComponentItemFactory1_20_5(plugin);
        } else if (VersionHelper.isOrAbove1_20()) {
            return new UniversalItemFactory(plugin);
        }
        throw new IllegalStateException("Unsupported server version: " + VersionHelper.MINECRAFT_VERSION.version());
    }

    public void resetRecipeIngredientSources(ExternalItemSource<ItemStack>[] recipeIngredientSources) {
        if (recipeIngredientSources == null || recipeIngredientSources.length == 0) {
            this.recipeIngredientSources = null;
            this.hasExternalRecipeSource = false;
        } else {
            this.recipeIngredientSources = recipeIngredientSources;
            this.hasExternalRecipeSource = true;
        }
    }

    @Override
    protected boolean isEmpty(W item) {
        return FastNMS.INSTANCE.method$ItemStack$isEmpty(item.getLiteralObject());
    }

    @SuppressWarnings("deprecation")
    @Override
    protected byte[] toByteArray(W item) {
        return Bukkit.getUnsafe().serializeItem(item.getItem());
    }

    @Override
    protected boolean isBlockItem(W item) {
        return CoreReflections.clazz$BlockItem.isInstance(FastNMS.INSTANCE.method$ItemStack$getItem(item.getLiteralObject()));
    }

    @Override
    protected Key vanillaId(W item) {
        Object i = FastNMS.INSTANCE.method$ItemStack$getItem(item.getLiteralObject());
        if (i == null) return ItemKeys.AIR;
        return KeyUtils.resourceLocationToKey(FastNMS.INSTANCE.method$Registry$getKey(MBuiltInRegistries.ITEM, i));
    }

    @Override
    protected Key id(W item) {
        if (FastNMS.INSTANCE.method$ItemStack$isEmpty(item.getLiteralObject())) {
            return ItemKeys.AIR;
        }
        return customId(item).orElse(vanillaId(item));
    }

    @Override
    protected ItemStack getItem(W item) {
        return item.getItem();
    }

    @Override
    protected UniqueKey recipeIngredientID(W item) {
        if (FastNMS.INSTANCE.method$ItemStack$isEmpty(item.getLiteralObject())) {
            return null;
        }
        if (this.hasExternalRecipeSource) {
           for (ExternalItemSource<ItemStack> source : this.recipeIngredientSources) {
               String id = source.id(item.getItem());
               if (id != null) {
                   return UniqueKey.create(Key.of(source.plugin(), StringUtils.toLowerCase(id)));
               }
           }
        }
        return UniqueKey.create(id(item));
    }

    @Override
    protected boolean hasItemTag(W item, Key itemTag) {
        Object literalObject = item.getLiteralObject();
        Object tag = ItemTags.getOrCreate(itemTag);
        return FastNMS.INSTANCE.method$ItemStack$is(literalObject, tag);
    }

    @Override
    protected void setJavaComponent(W item, Object type, Object value) {
        throw new UnsupportedOperationException("This feature is only available on 1.20.5+");
    }

    @Override
    protected void setJsonComponent(W item, Object type, JsonElement value) {
        throw new UnsupportedOperationException("This feature is only available on 1.20.5+");
    }

    @Override
    protected void setNBTComponent(W item, Object type, Tag value) {
        throw new UnsupportedOperationException("This feature is only available on 1.20.5+");
    }

    @Override
    protected Object getJavaComponent(W item, Object type) {
        throw new UnsupportedOperationException("This feature is only available on 1.20.5+");
    }

    @Override
    protected JsonElement getJsonComponent(W item, Object type) {
        throw new UnsupportedOperationException("This feature is only available on 1.20.5+");
    }

    @Override
    public Object getNBTComponent(W item, Object type) {
        throw new UnsupportedOperationException("This feature is only available on 1.20.5+");
    }

    @Override
    protected Tag getSparrowNBTComponent(W item, Object type) {
        throw new UnsupportedOperationException("This feature is only available on 1.20.5+");
    }

    @Override
    protected void resetComponent(W item, Object type) {
        throw new UnsupportedOperationException("This feature is only available on 1.20.5+");
    }

    @Override
    protected boolean hasNonDefaultComponent(W item, Object type) {
        throw new UnsupportedOperationException("This feature is only available on 1.20.5+");
    }

    @Override
    protected void setComponent(W item, Object type, Object value) {
        throw new UnsupportedOperationException("This feature is only available on 1.20.5+");
    }

    @Override
    protected Object getExactComponent(W item, Object type) {
        throw new UnsupportedOperationException("This feature is only available on 1.20.5+");
    }

    @Override
    protected void setExactComponent(W item, Object type, Object value) {
        throw new UnsupportedOperationException("This feature is only available on 1.20.5+");
    }

    @Override
    protected boolean hasComponent(W item, Object type) {
        throw new UnsupportedOperationException("This feature is only available on 1.20.5+");
    }

    @Override
    protected void removeComponent(W item, Object type) {
        throw new UnsupportedOperationException("This feature is only available on 1.20.5+");
    }

    @Override
    protected Optional<String> tooltipStyle(W item) {
        throw new UnsupportedOperationException("This feature is only available on 1.21.2+");
    }

    @Override
    protected void tooltipStyle(W item, String data) {
        throw new UnsupportedOperationException("This feature is only available on 1.21.2+");
    }

    @Override
    protected Optional<JukeboxPlayable> jukeboxSong(W item) {
        throw new UnsupportedOperationException("This feature is only available on 1.21+");
    }

    @Override
    protected void jukeboxSong(W item, JukeboxPlayable data) {
        throw new UnsupportedOperationException("This feature is only available on 1.21+");
    }

    @Override
    protected Optional<Boolean> glint(W item) {
        throw new UnsupportedOperationException("This feature is only available on 1.20.5+");
    }

    @Override
    protected void glint(W item, Boolean glint) {
        throw new UnsupportedOperationException("This feature is only available on 1.20.5+");
    }

    @Override
    protected Optional<String> itemModel(W item) {
        throw new UnsupportedOperationException("This feature is only available on 1.21.2+");
    }

    @Override
    protected void itemModel(W item, String data) {
        throw new UnsupportedOperationException("This feature is only available on 1.21.2+");
    }

    @Override
    protected Optional<EquipmentData> equippable(W item) {
        throw new UnsupportedOperationException("This feature is only available on 1.21.2+");
    }

    @Override
    protected void equippable(W item, EquipmentData data) {
        throw new UnsupportedOperationException("This feature is only available on 1.21.2+");
    }
}
