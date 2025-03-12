package net.momirealms.craftengine.core.block.behavior;

import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;
import net.momirealms.craftengine.shared.block.BlockBehavior;
import net.momirealms.craftengine.shared.block.EmptyBlockBehavior;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class BlockBehaviors {

    public static void register(Key key, BlockBehaviorFactory factory) {
        Holder.Reference<BlockBehaviorFactory> holder = ((WritableRegistry<BlockBehaviorFactory>) BuiltInRegistries.BLOCK_BEHAVIOR_FACTORY)
                .registerForHolder(new ResourceKey<>(Registries.BLOCK_BEHAVIOR_FACTORY.location(), key));
        holder.bindValue(factory);
    }

    public static BlockBehavior fromMap(CustomBlock block, @Nullable Map<String, Object> map) {
        if (map == null) return EmptyBlockBehavior.INSTANCE;
        String type = (String) map.getOrDefault("type", "empty");
        if (type == null) {
            throw new NullPointerException("behavior type cannot be null");
        }
        Key key = Key.withDefaultNamespace(type, "craftengine");
        BlockBehaviorFactory factory = BuiltInRegistries.BLOCK_BEHAVIOR_FACTORY.getValue(key);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown behavior type: " + type);
        }
        return factory.create(block, map);
    }
}
