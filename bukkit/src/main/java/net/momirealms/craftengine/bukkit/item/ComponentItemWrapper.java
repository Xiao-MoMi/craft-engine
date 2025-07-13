package net.momirealms.craftengine.bukkit.item;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBuiltInRegistries;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MRegistryOps;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.item.ItemWrapper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.sparrow.nbt.Tag;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class ComponentItemWrapper implements ItemWrapper<ItemStack> {
    private final ItemStack item;
    private final Object handle;

    public ComponentItemWrapper(final ItemStack item) {
        this.item = ItemStackUtils.ensureCraftItemStack(item);
        this.handle = FastNMS.INSTANCE.field$CraftItemStack$handle(this.item);
    }

    public ComponentItemWrapper(final ItemStack item, int count) {
        this.item = ItemStackUtils.ensureCraftItemStack(item);
        this.item.setAmount(count);
        this.handle = FastNMS.INSTANCE.field$CraftItemStack$handle(this.item);
    }

    public void removeComponent(Object type) {
        FastNMS.INSTANCE.method$ItemStack$removeComponent(this.getLiteralObject(), ensureDataComponentType(type));
    }

    public void resetComponent(Object type) {
        Object item = FastNMS.INSTANCE.method$ItemStack$getItem(this.getLiteralObject());
        Object componentMap = FastNMS.INSTANCE.method$Item$components(item);
        Object componentType = ensureDataComponentType(type);
        Object defaultComponent = FastNMS.INSTANCE.method$DataComponentMap$get(componentMap, componentType);
        FastNMS.INSTANCE.method$ItemStack$setComponent(this.getLiteralObject(), componentType, defaultComponent);
    }

    public void setComponent(Object type, final Object value) {
        if (value instanceof JsonElement jsonElement) {
            setJsonComponent(type, jsonElement);
        } else if (CoreReflections.clazz$Tag.isInstance(value)) {
            setNBTComponent(type, value);
        } else if (value instanceof Tag tag) {
            setSparrowNBTComponent(type, tag);
        } else {
            setJavaComponent(type, value);
        }
    }

    public Object getComponentExact(Object type) {
        return FastNMS.INSTANCE.method$ItemStack$getComponent(getLiteralObject(), ensureDataComponentType(type));
    }

    public <T> Optional<T> getJavaComponent(Object type) {
        return getComponentInternal(type, MRegistryOps.JAVA);
    }

    public Optional<JsonElement> getJsonComponent(Object type) {
        return getComponentInternal(type, MRegistryOps.JSON);
    }

    public Optional<Object> getNBTComponent(Object type) {
        return getComponentInternal(type, MRegistryOps.NBT);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public Optional<Tag> getSparrowNBTComponent(Object type) {
        Object componentType = ensureDataComponentType(type);
        Codec codec = FastNMS.INSTANCE.method$DataComponentType$codec(componentType);
        try {
            Object componentData = FastNMS.INSTANCE.method$ItemStack$getComponent(getLiteralObject(), componentType);
            if (componentData == null) return Optional.empty();
            DataResult<Tag> result = codec.encodeStart(MRegistryOps.SPARROW_NBT, componentData);
            return result.result().map(Tag::copy);
        } catch (Throwable t) {
            throw new RuntimeException("Cannot read component " + type.toString(), t);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private <T> Optional<T> getComponentInternal(Object type, DynamicOps ops) {
        Object componentType = ensureDataComponentType(type);
        Codec codec = FastNMS.INSTANCE.method$DataComponentType$codec(componentType);
        try {
            Object componentData = FastNMS.INSTANCE.method$ItemStack$getComponent(getLiteralObject(), componentType);
            if (componentData == null) return Optional.empty();
            DataResult<Object> result = codec.encodeStart(ops, componentData);
            return (Optional<T>) result.result();
        } catch (Throwable t) {
            throw new RuntimeException("Cannot read component " + type.toString(), t);
        }
    }

    public boolean hasComponent(Object type) {
        return FastNMS.INSTANCE.method$ItemStack$hasComponent(getLiteralObject(), ensureDataComponentType(type));
    }

    public void setComponentExact(Object type, final Object value) {
        FastNMS.INSTANCE.method$ItemStack$setComponent(this.getLiteralObject(), ensureDataComponentType(type), value);
    }

    public void setJavaComponent(Object type, Object value) {
        setComponentInternal(type, MRegistryOps.JAVA, value);
    }

    public void setJsonComponent(Object type, JsonElement value) {
        setComponentInternal(type, MRegistryOps.JSON, value);
    }

    public void setNBTComponent(Object type, Object value) {
       setComponentInternal(type, MRegistryOps.NBT, value);
    }

    public void setSparrowNBTComponent(Object type, Tag value) {
        setComponentInternal(type, MRegistryOps.SPARROW_NBT, value);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void setComponentInternal(Object type, DynamicOps ops, Object value) {
        if (value == null) return;
        Object componentType = ensureDataComponentType(type);
        if (componentType == null) {
            return;
        }
        Codec codec = FastNMS.INSTANCE.method$DataComponentType$codec(componentType);
        try {
            DataResult<Object> result = codec.parse(ops, value);
            if (result.isError()) {
                throw new IllegalArgumentException(result.toString());
            }
            result.result().ifPresent(it -> FastNMS.INSTANCE.method$ItemStack$setComponent(this.getLiteralObject(), componentType, it));
        } catch (Throwable t) {
            throw new RuntimeException("Cannot parse component " + type.toString(), t);
        }
    }

    private Object ensureDataComponentType(Object type) {
        if (!CoreReflections.clazz$DataComponentType.isInstance(type)) {
            Key key = Key.of(type.toString());
            return FastNMS.INSTANCE.method$Registry$getValue(MBuiltInRegistries.DATA_COMPONENT_TYPE, KeyUtils.toResourceLocation(key));
        }
        return type;
    }

    @Override
    public ItemWrapper<ItemStack> copyWithCount(int count) {
        ItemStack copied = this.item.clone();
        copied.setAmount(count);
        return new ComponentItemWrapper(copied);
    }

    @Override
    public ItemStack getItem() {
        return this.item;
    }

    @Override
    public Object getLiteralObject() {
        return this.handle;
    }

    @Override
    public int count() {
        return this.item.getAmount();
    }

    @Override
    public void count(int amount) {
        this.item.setAmount(Math.max(amount, 0));
    }

    @Override
    public void shrink(int amount) {
        count(count() - amount);
    }
}
