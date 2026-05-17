package net.momirealms.craftengine.core.block.entity.render.tint;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.Map;

public final class BlockEntityTintSources {
    public static final BlockEntityTintSourceType<DefaultBlockEntityTintSource> DEFAULT = register(Key.ce("default"), DefaultBlockEntityTintSourceConfig.FACTORY);

    private BlockEntityTintSources() {}

    public static <T extends BlockEntityTintSource> BlockEntityTintSourceType<T> register(Key key, BlockEntityTintSourceConfigFactory<T> factory) {
        BlockEntityTintSourceType<T> type = new BlockEntityTintSourceType<>(key, factory);
        ((WritableRegistry<BlockEntityTintSourceType<? extends BlockEntityTintSource>>) BuiltInRegistries.BLOCK_ENTITY_TINT_SOURCE_TYPE)
                .register(ResourceKey.create(Registries.BLOCK_ENTITY_TINT_SOURCE_TYPE.location(), key), type);
        return type;
    }

    public static BlockEntityTintSourceConfig<? extends BlockEntityTintSource> fromConfig(ConfigValue value) {
        if (value.is(Map.class)) {
            return fromConfig(value.getAsSection());
        } else {
            return DefaultBlockEntityTintSourceConfig.create(value.getAsList(ConfigValue::getAsIdentifier), 0);
        }
    }

    public static BlockEntityTintSourceConfig<? extends BlockEntityTintSource> fromConfig(ConfigSection section) {
        String typeName = section.getString("type", "default");
        Key type = Key.ce(typeName);
        BlockEntityTintSourceType<? extends BlockEntityTintSource> sourceType = BuiltInRegistries.BLOCK_ENTITY_TINT_SOURCE_TYPE.getValue(type);
        if (sourceType == null) {
            throw new KnownResourceException("resource.block.entity_renderer.tint_source.unknown_type", section.assemblePath("type"), type.asString());
        }
        return sourceType.factory().create(section);
    }
}
