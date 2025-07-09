package net.momirealms.craftengine.core.block.behavior;

import net.momirealms.craftengine.core.block.BlockBehavior;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.ResourceKey;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class BlockBehaviors {
    public static final Key EMPTY = Key.from("craftengine:empty");

    public static void register(Key key, BlockBehaviorFactory factory) {
        Holder.Reference<BlockBehaviorFactory> holder = ((WritableRegistry<BlockBehaviorFactory>) BuiltInRegistries.BLOCK_BEHAVIOR_FACTORY)
                .registerForHolder(new ResourceKey<>(Registries.BLOCK_BEHAVIOR_FACTORY.location(), key));
        holder.bindValue(factory);
    }

    public static BlockBehavior fromMap(CustomBlock block, @Nullable Map<String, Object> map) {
        if (map == null || map.isEmpty()) return EmptyBlockBehavior.INSTANCE;
        String type = ResourceConfigUtils.requireNonEmptyStringOrThrow(map.get("type"), "warning.config.block.behavior.missing_type");
        Key key = Key.withDefaultNamespace(type, Key.DEFAULT_NAMESPACE);
        BlockBehaviorFactory factory = BuiltInRegistries.BLOCK_BEHAVIOR_FACTORY.getValue(key);
        if (factory == null) {
            throw new LocalizedResourceConfigException("warning.config.block.behavior.invalid_type", type);
        }
        return factory.create(block, map);
    }
}
