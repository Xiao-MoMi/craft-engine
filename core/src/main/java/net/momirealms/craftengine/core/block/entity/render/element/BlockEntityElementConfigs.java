package net.momirealms.craftengine.core.block.entity.render.element;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.Optional;

public abstract class BlockEntityElementConfigs {
    protected BlockEntityElementConfigs() {}

    public static <E extends ConstantBlockEntityElement> BlockEntityElementConfigType<E> register(Key key, BlockEntityElementConfigFactory<E> factory) {
        BlockEntityElementConfigType<E> type = new BlockEntityElementConfigType<>(key, factory);
        ((WritableRegistry<BlockEntityElementConfigType<? extends BlockEntityElement>>) BuiltInRegistries.BLOCK_ENTITY_ELEMENT_TYPE)
                .register(ResourceKey.create(Registries.BLOCK_ENTITY_ELEMENT_TYPE.location(), key), type);
        return type;
    }

    public static <E extends ConstantBlockEntityElement> BlockEntityElementConfig<E> fromConfig(ConfigSection section) {
        Key type = getOrGuessType(section);
        @SuppressWarnings("unchecked")
        BlockEntityElementConfigType<E> configType = (BlockEntityElementConfigType<E>) BuiltInRegistries.BLOCK_ENTITY_ELEMENT_TYPE.getValue(type);
        if (configType == null) {
            throw new KnownResourceException("resource.block.state.entity_renderer.unknown_type", section.assemblePath("type"), type.asString());
        }
        return configType.factory().create(section);
    }

    private static Key getOrGuessType(ConfigSection section) {
        return Key.ce(Optional.ofNullable(section.getString("type")).orElseGet(() -> {
            if (section.containsKey("text")) {
                return "text_display";
            } else if (section.containsKey("item")) {
                return "item_display";
            } else {
                // 到这里必定抛出异常
                return section.getNonNullString("type");
            }
        }));
    }
}
