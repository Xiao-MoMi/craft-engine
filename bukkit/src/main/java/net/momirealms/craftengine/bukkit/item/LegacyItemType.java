package net.momirealms.craftengine.bukkit.item;

import com.google.gson.JsonElement;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBuiltInRegistries;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.item.ItemType;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.sparrow.nbt.Tag;

import java.util.Optional;

public class LegacyItemType implements ItemType {
    private final Object item;

    public LegacyItemType(Object item) {
        this.item = item;
    }

    @Override
    public Key id() {
        return KeyUtils.resourceLocationToKey(FastNMS.INSTANCE.method$Registry$getKey(MBuiltInRegistries.ITEM, this.item));
    }

    @Override
    public Object getExactComponent(Object type) {
        throw new UnsupportedOperationException("1.20.5");
    }

    @Override
    public <T> Optional<T> getJavaComponent(Object type) {
        throw new UnsupportedOperationException("1.20.5");
    }

    @Override
    public Optional<JsonElement> getJsonComponent(Object type) {
        throw new UnsupportedOperationException("1.20.5");
    }

    @Override
    public Optional<Object> getNBTComponent(Object type) {
        throw new UnsupportedOperationException("1.20.5");
    }

    @Override
    public Optional<Tag> getSparrowNBTComponent(Object type) {
        throw new UnsupportedOperationException("1.20.5");
    }
}
