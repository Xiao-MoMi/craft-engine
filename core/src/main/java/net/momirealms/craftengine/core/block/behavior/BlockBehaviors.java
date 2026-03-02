package net.momirealms.craftengine.core.block.behavior;

import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;
import org.jetbrains.annotations.Nullable;

public class BlockBehaviors {
    public static final BlockBehaviorType<EmptyBlockBehavior> EMPTY = register(Key.ce("empty"), (block, args) -> EmptyBlockBehavior.INSTANCE);

    protected BlockBehaviors() {
    }

    public static <T extends BlockBehavior> BlockBehaviorType<T> register(Key key, BlockBehaviorFactory<T> factory) {
        BlockBehaviorType<T> type = new BlockBehaviorType<>(key, factory);
        ((WritableRegistry<BlockBehaviorType<? extends BlockBehavior>>) BuiltInRegistries.BLOCK_BEHAVIOR_TYPE)
                .register(ResourceKey.create(Registries.BLOCK_BEHAVIOR_TYPE.location(), key), type);
        return type;
    }

    public static BlockBehavior fromConfig(CustomBlock block, @Nullable ConfigSection section) {
        if (section == null || section.values().isEmpty()) return EmptyBlockBehavior.INSTANCE;
        Key type = section.getNonNullIdentifier("type");
        BlockBehaviorType<? extends BlockBehavior> factory = BuiltInRegistries.BLOCK_BEHAVIOR_TYPE.getValue(type);
        if (factory == null) {
            throw new LocalizedResourceConfigException("warning.config.block.behavior.invalid_type", type.toString());
        }
        return factory.factory().create(block, section);
    }
}
