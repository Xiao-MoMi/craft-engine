package net.momirealms.craftengine.core.block;

import net.momirealms.craftengine.core.block.setting.BlockSettings;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.Map;

public final class EmptyBlockDefinition extends AbstractBlockDefinition {
    public static final EmptyBlockDefinition INSTANCE;
    public static final ImmutableBlockState STATE;

    static {
        Holder.Reference<BlockDefinition> holder = ((WritableRegistry<BlockDefinition>) BuiltInRegistries.BLOCK).registerForHolder(ResourceKey.create(BuiltInRegistries.BLOCK.key().location(), Key.withCraftEngineNamespace("empty")));
        INSTANCE = new EmptyBlockDefinition(holder);
        holder.bindValue(INSTANCE);
        STATE = INSTANCE.defaultState();
        STATE.setSettings(BlockSettings.of());
    }

    private EmptyBlockDefinition(Holder.Reference<BlockDefinition> holder) {
        super(holder, new BlockStateVariantProvider(holder, ImmutableBlockState::new, Map.of()), Map.of(), null);
    }

    public static void init() {
    }
}
