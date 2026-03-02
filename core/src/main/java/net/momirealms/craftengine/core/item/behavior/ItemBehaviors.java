package net.momirealms.craftengine.core.item.behavior;

import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.ResourceKey;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Map;

public class ItemBehaviors {
    public static final ItemBehaviorType<EmptyItemBehavior> EMPTY = register(Key.withDefaultNamespace("empty", Key.DEFAULT_NAMESPACE), EmptyItemBehavior.FACTORY);

    public static <T extends ItemBehavior> ItemBehaviorType<T> register(Key key, ItemBehaviorFactory<T> factory) {
        ItemBehaviorType<T> type = new ItemBehaviorType<>(key, factory);
        ((WritableRegistry<ItemBehaviorType<? extends ItemBehavior>>) BuiltInRegistries.ITEM_BEHAVIOR_TYPE)
                .register(ResourceKey.create(Registries.ITEM_BEHAVIOR_TYPE.location(), key), type);
        return type;
    }

    public static ItemBehavior fromConfig(Pack pack, Path path, String node, Key id, @Nullable ConfigSection section) {
        if (section == null || section.values().isEmpty()) return EmptyItemBehavior.INSTANCE;
        Key type = section.getNonNullIdentifier("type");
        ItemBehaviorType<? extends ItemBehavior> behaviorType = BuiltInRegistries.ITEM_BEHAVIOR_TYPE.getValue(type);
        if (behaviorType == null) {
            throw new LocalizedResourceConfigException("warning.config.item.behavior.invalid_type", type.asString());
        }
        return behaviorType.factory().create(pack, path, node, id, section);
    }
}