package net.momirealms.craftengine.bukkit.plugin.injector;

import net.momirealms.craftengine.bukkit.util.RegistryUtils;
import net.momirealms.craftengine.bukkit.world.gen.BlockStateProviderAdapter;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.proxy.minecraft.core.registries.BuiltInRegistriesProxy;

public final class BlockStateProviderInjector {
    private BlockStateProviderInjector() {
    }

    public static void init() {
        Object simple = BlockStateProviderAdapter.SIMPLE;
        Object weighted = BlockStateProviderAdapter.WEIGHTED;
        Object rotated = BlockStateProviderAdapter.ROTATED;
        Object randomizedInt = BlockStateProviderAdapter.RANDOMIZED_INT;
        Object registry = BuiltInRegistriesProxy.BLOCKSTATE_PROVIDER_TYPE;
        RegistryUtils.registerAndFreeze(registry, () -> {
            RegistryUtils.registerBoundValue(registry, Key.ce("simple_state_provider"), simple);
            RegistryUtils.registerBoundValue(registry, Key.ce("weighted_state_provider"), weighted);
            RegistryUtils.registerBoundValue(registry, Key.ce("rotated_block_provider"), rotated);
            RegistryUtils.registerBoundValue(registry, Key.ce("randomized_int_state_provider"), randomizedInt);
        });
    }
}
