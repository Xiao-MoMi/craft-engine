package net.momirealms.craftengine.bukkit.item.factory;

import com.google.gson.JsonElement;
import net.momirealms.craftengine.bukkit.item.BukkitItemWrapper;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.bukkit.util.ItemTags;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemFactory;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.item.component.value.JukeboxPlayable;
import net.momirealms.craftengine.core.item.setting.value.EquipmentData;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.core.RegistryProxy;
import net.momirealms.craftengine.proxy.minecraft.core.registries.BuiltInRegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.BlockItemProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;
import org.bukkit.Bukkit;

import java.util.Objects;
import java.util.Optional;

public abstract class BukkitItemFactory<W extends BukkitItemWrapper> extends ItemFactory<W> {

    protected BukkitItemFactory(CraftEngine plugin) {
        super(plugin);
    }

    public static BukkitItemFactory<? extends BukkitItemWrapper> create(CraftEngine plugin) {
        Objects.requireNonNull(plugin, "plugin");
        if (VersionHelper.isOrAbove1_21_5) {
            return new ComponentItemFactory1_21_5(plugin);
        } else if (VersionHelper.isOrAbove1_21_4) {
            return new ComponentItemFactory1_21_4(plugin);
        } else if (VersionHelper.isOrAbove1_21_2) {
            return new ComponentItemFactory1_21_2(plugin);
        } else if (VersionHelper.isOrAbove1_21) {
            return new ComponentItemFactory1_21(plugin);
        } else if (VersionHelper.isOrAbove1_20_5) {
            return new ComponentItemFactory1_20_5(plugin);
        } else if (VersionHelper.isOrAbove1_20) {
            return new UniversalItemFactory(plugin);
        }
        throw new IllegalStateException("Unsupported server version: " + VersionHelper.MINECRAFT_VERSION.version());
    }

    @Override
    protected boolean isEmpty(W item) {
        return ItemStackProxy.INSTANCE.isEmpty(item.minecraftItem());
    }

    @SuppressWarnings("deprecation")
    @Override
    protected byte[] toByteArray(W item) {
        return Bukkit.getUnsafe().serializeItem(ItemStackProxy.INSTANCE.getBukkitStack(item.minecraftItem()));
    }

    @Override
    protected CompoundTag toNBT(W item) {
        return (CompoundTag) ItemStackUtils.saveMinecraftItemStackAsTag(item.minecraftItem());
    }

    @Override
    protected boolean isBlockItem(W item) {
        return BlockItemProxy.CLASS.isInstance(ItemStackProxy.INSTANCE.getItem(item.minecraftItem()));
    }

    @Override
    protected Key vanillaId(W item) {
        Object i = ItemStackProxy.INSTANCE.getItem(item.minecraftItem());
        if (i == null) return ItemKeys.AIR;
        return KeyUtils.identifierToKey(RegistryProxy.INSTANCE.getKey(BuiltInRegistriesProxy.ITEM, i));
    }

    @Override
    protected Key id(W item) {
        if (ItemStackProxy.INSTANCE.isEmpty(item.minecraftItem())) {
            return ItemKeys.AIR;
        }
        return customId(item).orElse(vanillaId(item));
    }

    @Override
    protected boolean hasItemTag(W item, Key itemTag) {
        Object minecraftItem = item.minecraftItem();
        Object tag = ItemTags.getOrCreate(itemTag);
        return ItemStackProxy.INSTANCE.is$0(minecraftItem, tag);
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
    protected void setSparrowTagComponent(W item, Object type, Tag value) {
        throw new UnsupportedOperationException("This feature is only available on 1.20.5+");
    }

    @Override
    protected void setMinecraftTagComponent(W item, Object type, Object value) {
        throw new UnsupportedOperationException("This feature is only available on 1.20.5+");
    }

    @Override
    protected Object getComponentAsJava(W item, Object type) {
        throw new UnsupportedOperationException("This feature is only available on 1.20.5+");
    }

    @Override
    protected JsonElement getComponentAsJson(W item, Object type) {
        throw new UnsupportedOperationException("This feature is only available on 1.20.5+");
    }

    @Override
    public Object getComponentAsMinecraftTag(W item, Object type) {
        throw new UnsupportedOperationException("This feature is only available on 1.20.5+");
    }

    @Override
    protected Tag getComponentAsSparrowTag(W item, Object type) {
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
    protected Optional<W> useRemainder(W item) {
        throw new UnsupportedOperationException("This feature is only available on 1.21.2+");
    }

    @Override
    protected void useRemainder(W item, Item data, int count) {
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
