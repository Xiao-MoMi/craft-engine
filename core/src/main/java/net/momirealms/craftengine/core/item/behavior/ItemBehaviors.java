package net.momirealms.craftengine.core.item.behavior;

import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.nio.file.Path;
import java.util.Map;

public class ItemBehaviors {

    public static void register(Key key, ItemBehaviorFactory factory) {
        Holder.Reference<ItemBehaviorFactory> holder = ((WritableRegistry<ItemBehaviorFactory>) BuiltInRegistries.ITEM_BEHAVIOR_FACTORY)
                .registerForHolder(new ResourceKey<>(Registries.ITEM_BEHAVIOR_FACTORY.location(), key));
        holder.bindValue(factory);
    }

    public static ItemBehavior fromMap(Pack pack, Path path, Key id, Map<String, Object> map) {
        String type = (String) map.getOrDefault("type", "empty");
        if (type == null) {
            throw new NullPointerException("behavior type cannot be null");
        }
        Key key = Key.withDefaultNamespace(type, "craftengine");
        ItemBehaviorFactory factory = BuiltInRegistries.ITEM_BEHAVIOR_FACTORY.getValue(key);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown behavior type: " + type);
        }
        return factory.create(pack, path, id, map);
    }
}