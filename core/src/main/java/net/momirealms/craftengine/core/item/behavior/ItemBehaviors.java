package net.momirealms.craftengine.core.item.behavior;

import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.nio.file.Path;

public abstract class ItemBehaviors {
    public static final ItemBehaviorType<EmptyItemBehavior> EMPTY = register(Key.withDefaultNamespace("empty", Key.CRAFTENGINE_NAMESPACE), EmptyItemBehavior.FACTORY);

    protected ItemBehaviors() {}

    public static <T extends ItemBehavior> ItemBehaviorType<T> register(Key key, ItemBehaviorFactory<T> factory) {
        ItemBehaviorType<T> type = new ItemBehaviorType<>(key, factory);
        ((WritableRegistry<ItemBehaviorType<? extends ItemBehavior>>) BuiltInRegistries.ITEM_BEHAVIOR_TYPE)
                .register(ResourceKey.create(Registries.ITEM_BEHAVIOR_TYPE.location(), key), type);
        return type;
    }

    public static ItemBehavior fromConfig(Pack pack, Path path, Key id, ConfigSection section) {
        String type = section.getNonEmptyString("type");
        Key key = Key.ce(type);
        ItemBehaviorType<? extends ItemBehavior> behaviorType = BuiltInRegistries.ITEM_BEHAVIOR_TYPE.getValue(key);
        if (behaviorType == null) {
            throw new KnownResourceException("resource.item.behavior.unknown_type", section.assemblePath("type"), key.asString());
        }
        return behaviorType.factory().create(pack, path, id, section);
    }
}