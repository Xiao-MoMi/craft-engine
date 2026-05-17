package net.momirealms.craftengine.core.block.behavior;

import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

public abstract class BlockBehaviors {
    protected BlockBehaviors() {
    }

    public static <T extends BlockBehavior> BlockBehaviorType<T> register(Key key, BlockBehaviorFactory<T> factory) {
        BlockBehaviorType<T> type = new BlockBehaviorType<>(key, factory);
        ((WritableRegistry<BlockBehaviorType<? extends BlockBehavior>>) BuiltInRegistries.BLOCK_BEHAVIOR_TYPE)
                .register(ResourceKey.create(Registries.BLOCK_BEHAVIOR_TYPE.location(), key), type);
        return type;
    }

    public static BlockBehavior fromConfig(BlockDefinition block, ConfigSection section) {
        String type = section.getNonEmptyString("type");
        Key key = Key.ce(type);
        BlockBehaviorType<? extends BlockBehavior> factory = BuiltInRegistries.BLOCK_BEHAVIOR_TYPE.getValue(key);
        if (factory == null) {
            throw new KnownResourceException("resource.block.behavior.unknown_type", section.assemblePath("type"), key.asString());
        }
        return factory.factory().create(block, section);
    }
}
