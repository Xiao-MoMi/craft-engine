package net.momirealms.craftengine.bukkit.item;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.momirealms.craftengine.bukkit.util.RegistryOps;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.bukkit.util.RegistryUtils;
import net.momirealms.craftengine.core.item.ItemType;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.core.RegistryProxy;
import net.momirealms.craftengine.proxy.minecraft.core.component.DataComponentGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.core.component.DataComponentMapProxy;
import net.momirealms.craftengine.proxy.minecraft.core.component.DataComponentTypeProxy;
import net.momirealms.craftengine.proxy.minecraft.core.registries.BuiltInRegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemProxy;
import net.momirealms.sparrow.nbt.Tag;

import java.util.Optional;

public class ComponentItemType implements ItemType {
    private final Object item;

    public ComponentItemType(Object item) {
        this.item = item;
    }

    @Override
    public Key id() {
        return KeyUtils.identifierToKey(RegistryProxy.INSTANCE.getKey(BuiltInRegistriesProxy.ITEM, this.item));
    }

    @Override
    public Object getExactComponent(Object type) {
        return getDefaultComponentInternal(type);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> getJavaComponent(Object type) {
        return (Optional<T>) getDefaultComponentInternal(type, RegistryOps.JAVA);
    }

    @Override
    public Optional<JsonElement> getJsonComponent(Object type) {
        return getDefaultComponentInternal(type, RegistryOps.JSON);
    }

    @Override
    public Optional<Object> getNBTComponent(Object type) {
        return getDefaultComponentInternal(type, RegistryOps.NBT);
    }

    @Override
    public Optional<Tag> getSparrowNBTComponent(Object type) {
        return getDefaultComponentInternal(type, RegistryOps.SPARROW_NBT).map(Tag::copy);
    }

    private <T> T getDefaultComponentInternal(Object type) {
        if (VersionHelper.isOrAbove1_21_5()) {
            return DataComponentGetterProxy.INSTANCE.get(ItemProxy.INSTANCE.components(this.item), type);
        } else {
            return DataComponentMapProxy.INSTANCE.get(ItemProxy.INSTANCE.components(this.item), type);
        }
    }

    private <T> Optional<T> getDefaultComponentInternal(Object type, DynamicOps<T> ops) {
        Object componentType = ensureDataComponentType(type);
        Codec<T> codec = DataComponentTypeProxy.INSTANCE.codec(componentType);
        try {
            T componentData = getDefaultComponentInternal(componentType);
            if (componentData == null) return Optional.empty();
            DataResult<T> result = codec.encodeStart(ops, componentData);
            return result.result();
        } catch (Throwable t) {
            throw new RuntimeException("Cannot read component " + type.toString(), t);
        }
    }

    private Object ensureDataComponentType(Object type) {
        if (!DataComponentTypeProxy.CLASS.isInstance(type)) {
            Key key = Key.of(type.toString());
            return RegistryUtils.getRegistryValue(BuiltInRegistriesProxy.DATA_COMPONENT_TYPE, KeyUtils.toIdentifier(key));
        }
        return type;
    }
}
