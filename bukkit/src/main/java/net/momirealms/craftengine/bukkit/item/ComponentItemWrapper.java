package net.momirealms.craftengine.bukkit.item;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.bukkit.util.RegistryOps;
import net.momirealms.craftengine.bukkit.util.RegistryUtils;
import net.momirealms.craftengine.core.item.ItemType;
import net.momirealms.craftengine.core.item.ItemWrapper;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.core.component.DataComponentGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.core.component.DataComponentMapProxy;
import net.momirealms.craftengine.proxy.minecraft.core.component.DataComponentTypeProxy;
import net.momirealms.craftengine.proxy.minecraft.core.registries.BuiltInRegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.nbt.TagProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.sparrow.nbt.Tag;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.Optional;

public final class ComponentItemWrapper extends BukkitItemWrapper {

    public ComponentItemWrapper(Object itemStack) {
        super(itemStack);
    }

    public ComponentItemWrapper(ItemStack itemStack) {
        super(itemStack);
    }

    public ItemType createItemType() {
        return new ComponentItemType(ItemStackProxy.INSTANCE.getItem(this.minecraftItem()));
    }

    @Override
    public ItemWrapper copy() {
        return new ComponentItemWrapper(ItemStackProxy.INSTANCE.copy(this.itemStack));
    }

    @Override
    public ItemWrapper copyWithCount(int count) {
        return new ComponentItemWrapper(ItemStackProxy.INSTANCE.copyWithCount(this.itemStack, count));
    }

    public void removeComponent(Object type) {
        ItemStackProxy.INSTANCE.remove(this.minecraftItem(), ensureDataComponentType(type));
    }

    public void resetComponent(Object type) {
        Object item = ItemStackProxy.INSTANCE.getItem(this.minecraftItem());
        Object componentMap = ItemProxy.INSTANCE.components(item);
        Object componentType = ensureDataComponentType(type);
        Object defaultComponent;
        if (VersionHelper.isOrAbove1_21_5) {
            defaultComponent = DataComponentGetterProxy.INSTANCE.get(componentMap, componentType);
        } else {
            defaultComponent = DataComponentMapProxy.INSTANCE.get(componentMap, componentType);
        }
        ItemStackProxy.INSTANCE.set(this.minecraftItem(), componentType, defaultComponent);
    }

    public void setComponent(Object type, final Object value) {
        if (value instanceof JsonElement jsonElement) {
            setJsonComponent(type, jsonElement);
        } else if (TagProxy.CLASS.isInstance(value)) {
            setNBTComponent(type, value);
        } else if (value instanceof Tag tag) {
            setSparrowNBTComponent(type, tag);
        } else {
            setJavaComponent(type, value);
        }
    }

    public Object getExactComponent(Object type) {
        return ItemStackProxy.INSTANCE.get(minecraftItem(), ensureDataComponentType(type));
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getComponentAsJava(Object type) {
        return (Optional<T>) getComponentInternal(type, RegistryOps.JAVA);
    }

    public Optional<JsonElement> getComponentAsJson(Object type) {
        return getComponentInternal(type, RegistryOps.JSON);
    }

    public Optional<Object> getComponentAsMinecraftTag(Object type) {
        return getComponentInternal(type, RegistryOps.NBT);
    }

    public Optional<Tag> getComponentAsSparrowTag(Object type) {
        return getComponentInternal(type, RegistryOps.SPARROW_NBT).map(Tag::copy);
    }

    private <T> Optional<T> getComponentInternal(Object type, DynamicOps<T> ops) {
        Object componentType = ensureDataComponentType(type);
        Codec<T> codec = DataComponentTypeProxy.INSTANCE.codecOrThrow(componentType);
        try {
            T componentData = ItemStackProxy.INSTANCE.get(minecraftItem(), componentType);
            if (componentData == null) return Optional.empty();
            DataResult<T> result = codec.encodeStart(ops, componentData);
            return result.result();
        } catch (Throwable t) {
            throw new RuntimeException("Cannot read component " + type.toString(), t);
        }
    }

    public boolean hasComponent(Object type) {
        return ItemStackProxy.INSTANCE.has(minecraftItem(), ensureDataComponentType(type));
    }

    public boolean hasNonDefaultComponent(Object type) {
        if (VersionHelper.isOrAbove1_21_4) {
            return ItemStackProxy.INSTANCE.hasNonDefault(minecraftItem(), ensureDataComponentType(type));
        } else {
            Object item = ItemStackProxy.INSTANCE.getItem(this.minecraftItem());
            Object componentMap = ItemProxy.INSTANCE.components(item);
            Object componentType = ensureDataComponentType(type);
            Object defaultComponent;
            if (VersionHelper.isOrAbove1_21_5) {
                defaultComponent = DataComponentGetterProxy.INSTANCE.get(componentMap, componentType);
            } else {
                defaultComponent = DataComponentMapProxy.INSTANCE.get(componentMap, componentType);
            }
            return !Objects.equals(defaultComponent, getExactComponent(componentType));
        }
    }

    public void setExactComponent(Object type, final Object value) {
        ItemStackProxy.INSTANCE.set(this.minecraftItem(), ensureDataComponentType(type), value);
    }

    public void setJavaComponent(Object type, Object value) {
        setComponentInternal(type, RegistryOps.JAVA, value);
    }

    public void setJsonComponent(Object type, JsonElement value) {
        setComponentInternal(type, RegistryOps.JSON, value);
    }

    public void setNBTComponent(Object type, Object value) {
        setComponentInternal(type, RegistryOps.NBT, value);
    }

    public void setSparrowNBTComponent(Object type, Tag value) {
        setComponentInternal(type, RegistryOps.SPARROW_NBT, value);
    }

    private <T> void setComponentInternal(Object type, DynamicOps<T> ops, T value) {
        if (value == null) return;
        Object componentType = ensureDataComponentType(type);
        if (componentType == null) {
            return;
        }
        Codec<T> codec = DataComponentTypeProxy.INSTANCE.codecOrThrow(componentType);
        try {
            DataResult<T> result = codec.parse(ops, value);
            if (result.isError()) {
                throw new IllegalArgumentException(result.toString());
            }
            result.result().ifPresent(it -> ItemStackProxy.INSTANCE.set(this.minecraftItem(), componentType, it));
        } catch (Throwable t) {
            throw new RuntimeException("Cannot parse component " + type.toString(), t);
        }
    }

    private Object ensureDataComponentType(Object type) {
        if (!DataComponentTypeProxy.CLASS.isInstance(type)) {
            return RegistryUtils.getRegistryValue(BuiltInRegistriesProxy.DATA_COMPONENT_TYPE, KeyUtils.toIdentifier(type.toString()));
        }
        return type;
    }
}
