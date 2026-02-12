package net.momirealms.craftengine.bukkit.item;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBuiltInRegistries;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MRegistryOps;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.item.ItemType;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.sparrow.nbt.Tag;

import java.util.Optional;

public class ComponentItemType implements ItemType {
    private final Object item;

    public ComponentItemType(Object item) {
        this.item = item;
    }

    @Override
    public Key id() {
        return KeyUtils.resourceLocationToKey(FastNMS.INSTANCE.method$Registry$getKey(MBuiltInRegistries.ITEM, this.item));
    }

    @Override
    public Object getExactComponent(Object type) {
        return getDefaultComponentInternal(type);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> getJavaComponent(Object type) {
        return (Optional<T>) getDefaultComponentInternal(type, MRegistryOps.JAVA);
    }

    @Override
    public Optional<JsonElement> getJsonComponent(Object type) {
        return getDefaultComponentInternal(type, MRegistryOps.JSON);
    }

    @Override
    public Optional<Object> getNBTComponent(Object type) {
        return getDefaultComponentInternal(type, MRegistryOps.NBT);
    }

    @Override
    public Optional<Tag> getSparrowNBTComponent(Object type) {
        return getDefaultComponentInternal(type, MRegistryOps.SPARROW_NBT).map(Tag::copy);
    }

    private Object getDefaultComponentInternal(Object type) {
        return FastNMS.INSTANCE.method$DataComponentMap$get(FastNMS.INSTANCE.method$Item$components(this.item), type);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private <T> Optional<T> getDefaultComponentInternal(Object type, DynamicOps<T> ops) {
        Object componentType = ensureDataComponentType(type);
        Codec codec = FastNMS.INSTANCE.method$DataComponentType$codec(componentType);
        try {
            Object componentData = getDefaultComponentInternal(componentType);
            if (componentData == null) return Optional.empty();
            DataResult<Object> result = codec.encodeStart(ops, componentData);
            return (Optional<T>) result.result();
        } catch (Throwable t) {
            throw new RuntimeException("Cannot read component " + type.toString(), t);
        }
    }

    private Object ensureDataComponentType(Object type) {
        if (!CoreReflections.clazz$DataComponentType.isInstance(type)) {
            Key key = Key.of(type.toString());
            return FastNMS.INSTANCE.method$Registry$getValue(MBuiltInRegistries.DATA_COMPONENT_TYPE, KeyUtils.toResourceLocation(key));
        }
        return type;
    }
}
