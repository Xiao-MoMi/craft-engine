package net.momirealms.craftengine.core.item.behavior;

import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemBehaviors {
    public static final Key EMPTY = Key.withDefaultNamespace("empty", Key.DEFAULT_NAMESPACE);

    public static void register(Key key, ItemBehaviorFactory factory) {
        ((WritableRegistry<ItemBehaviorFactory>) BuiltInRegistries.ITEM_BEHAVIOR_FACTORY)
                .register(ResourceKey.create(Registries.ITEM_BEHAVIOR_FACTORY.location(), key), factory);
    }

    public static ItemBehavior fromMap(Pack pack, Path path, Key id, Map<String, Object> map) {
        if (map == null || map.isEmpty()) return EmptyItemBehavior.INSTANCE;
        String type = ResourceConfigUtils.requireNonEmptyStringOrThrow(map.get("type"), "warning.config.item.behavior.missing_type");
        Key key = Key.withDefaultNamespace(type, Key.DEFAULT_NAMESPACE);
        ItemBehaviorFactory factory = BuiltInRegistries.ITEM_BEHAVIOR_FACTORY.getValue(key);
        if (factory == null) {
            throw new LocalizedResourceConfigException("warning.config.item.behavior.invalid_type", type);
        }
        return factory.create(pack, path, id, map);
    }

    public static List<ItemBehavior> fromList(Pack pack, Path path, Key id, List<Map<String, Object>> list) {
        List<ItemBehavior> behaviors = new ArrayList<>(list.size());
        for (Map<String, Object> map : list) {
            behaviors.add(fromMap(pack, path, id, map));
        }
        return behaviors;
    }

    @SuppressWarnings("unchecked")
    public static List<ItemBehavior> fromObj(Pack pack, Path path, Key id, Object behaviorObj) {
        if (behaviorObj instanceof Map<?,?>) {
            return List.of(fromMap(pack, path, id, MiscUtils.castToMap(behaviorObj, false)));
        } else if (behaviorObj instanceof List<?>) {
            return fromList(pack, path, id, (List<Map<String, Object>>) behaviorObj);
        } else {
            return List.of();
        }
    }
}