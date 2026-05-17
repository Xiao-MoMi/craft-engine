package net.momirealms.craftengine.core.entity.furniture.hitbox;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

public abstract class FurnitureHitBoxConfigs {
    protected FurnitureHitBoxConfigs() {}

    public static <H extends FurnitureHitBox> FurnitureHitboxConfigType<H> register(Key key, FurnitureHitBoxConfigFactory<H> factory) {
        FurnitureHitboxConfigType<H> type = new FurnitureHitboxConfigType<>(key, factory);
        ((WritableRegistry<FurnitureHitboxConfigType<? extends FurnitureHitBox>>) BuiltInRegistries.FURNITURE_HITBOX_TYPE)
                .register(ResourceKey.create(Registries.FURNITURE_HITBOX_TYPE.location(), key), type);
        return type;
    }

    public static FurnitureHitBoxConfig<? extends FurnitureHitBox> fromConfig(ConfigSection section) {
        String typeName = section.getString("type", "interaction");
        Key type = Key.ce(typeName);
        FurnitureHitboxConfigType<? extends FurnitureHitBox> configType = BuiltInRegistries.FURNITURE_HITBOX_TYPE.getValue(type);
        if (configType == null) {
            throw new KnownResourceException("resource.furniture.hitbox.unknown_type", section.assemblePath("type"), type.asString());
        }
        return configType.factory().create(section);
    }
}