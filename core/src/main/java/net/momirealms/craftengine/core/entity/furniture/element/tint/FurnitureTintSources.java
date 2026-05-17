package net.momirealms.craftengine.core.entity.furniture.element.tint;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.Map;

public final class FurnitureTintSources {
    public static final FurnitureTintSourceType<DefaultFurnitureTintSource> DEFAULT = register(Key.ce("default"), DefaultFurnitureTintSourceConfig.FACTORY);

    private FurnitureTintSources() {}

    public static <T extends FurnitureTintSource> FurnitureTintSourceType<T> register(Key key, FurnitureTintSourceConfigFactory<T> factory) {
        FurnitureTintSourceType<T> type = new FurnitureTintSourceType<>(key, factory);
        ((WritableRegistry<FurnitureTintSourceType<? extends FurnitureTintSource>>) BuiltInRegistries.FURNITURE_TINT_SOURCE_TYPE)
                .register(ResourceKey.create(Registries.FURNITURE_TINT_SOURCE_TYPE.location(), key), type);
        return type;
    }

    public static FurnitureTintSourceConfig<? extends FurnitureTintSource> fromConfig(ConfigValue value) {
        if (value.is(Map.class)) {
            return fromConfig(value.getAsSection());
        } else {
            return DefaultFurnitureTintSourceConfig.create(value.getAsList(ConfigValue::getAsIdentifier));
        }
    }

    public static FurnitureTintSourceConfig<? extends FurnitureTintSource> fromConfig(ConfigSection section) {
        String typeName = section.getString("type", "default");
        Key type = Key.ce(typeName);
        FurnitureTintSourceType<? extends FurnitureTintSource> sourceType = BuiltInRegistries.FURNITURE_TINT_SOURCE_TYPE.getValue(type);
        if (sourceType == null) {
            throw new KnownResourceException("resource.furniture.element.tint_source.unknown_type", section.assemblePath("type"), type.asString());
        }
        return sourceType.factory().create(section);
    }
}
