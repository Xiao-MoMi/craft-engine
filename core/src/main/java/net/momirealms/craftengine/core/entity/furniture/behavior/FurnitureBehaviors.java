package net.momirealms.craftengine.core.entity.furniture.behavior;

import net.momirealms.craftengine.core.entity.furniture.FurnitureDefinition;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Experimental
public abstract class FurnitureBehaviors {
    protected FurnitureBehaviors() {}

    public static FurnitureBehaviorTemplate fromConfig(FurnitureDefinition furniture, ConfigSection section) {
        String type = section.getNonEmptyString("type");
        Key key = Key.ce(type);
        FurnitureBehaviorType<?> furnitureBehaviorType = BuiltInRegistries.FURNITURE_BEHAVIOR_TYPE.getValue(key);
        if (furnitureBehaviorType == null) {
            throw new KnownResourceException("resource.furniture.behavior.unknown_type", section.assemblePath("type"), key.asString());
        }
        return furnitureBehaviorType.factory().create(furniture, section);
    }

    public static <T extends FurnitureBehaviorTemplate> FurnitureBehaviorType<T> register(Key id, FurnitureBehaviorFactory<T> factory) {
        FurnitureBehaviorType<T> type = new FurnitureBehaviorType<>(id, factory);
        ((WritableRegistry<FurnitureBehaviorType<? extends FurnitureBehaviorTemplate>>) BuiltInRegistries.FURNITURE_BEHAVIOR_TYPE)
                .register(ResourceKey.create(Registries.FURNITURE_BEHAVIOR_TYPE.location(), id), type);
        return type;
    }
}
