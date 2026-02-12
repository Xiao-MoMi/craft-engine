package net.momirealms.craftengine.core.entity.furniture.behavior;

import net.momirealms.craftengine.core.entity.furniture.CustomFurniture;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.ResourceKey;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@ApiStatus.Experimental
public class FurnitureBehaviors {

    protected FurnitureBehaviors() {}

    public static FurnitureBehavior fromMap(CustomFurniture furniture, @Nullable Map<String, Object> map) {
        if (map == null || map.isEmpty()) return new EmptyFurnitureBehavior(furniture);
        String type = ResourceConfigUtils.requireNonEmptyStringOrThrow(map.get("type"), "warning.config.furniture.behavior.missing_type");
        Key key = Key.withDefaultNamespace(type, Key.DEFAULT_NAMESPACE);
        FurnitureBehaviorType<?> furnitureBehaviorType = BuiltInRegistries.FURNITURE_BEHAVIOR_TYPE.getValue(key);
        if (furnitureBehaviorType == null) {
            throw new LocalizedResourceConfigException("warning.config.furniture.behavior.invalid_type", type);
        }
        return furnitureBehaviorType.factory().create(furniture, map);
    }

    public static <T extends FurnitureBehavior> FurnitureBehaviorType<T> register(Key id, FurnitureBehaviorFactory<T> factory) {
        FurnitureBehaviorType<T> type = new FurnitureBehaviorType<>(id, factory);
        ((WritableRegistry<FurnitureBehaviorType<? extends FurnitureBehavior>>) BuiltInRegistries.FURNITURE_BEHAVIOR_TYPE)
                .register(ResourceKey.create(Registries.FURNITURE_BEHAVIOR_TYPE.location(), id), type);
        return type;
    }
}
