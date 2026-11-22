package net.momirealms.craftengine.bukkit.plugin.injector;

import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.bukkit.world.gen.BlockStateProviderAdapter;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.proxy.minecraft.core.HolderProxy;
import net.momirealms.craftengine.proxy.minecraft.core.MappedRegistryProxy;
import net.momirealms.craftengine.proxy.minecraft.core.RegistryProxy;
import net.momirealms.craftengine.proxy.minecraft.core.registries.BuiltInRegistriesProxy;

import java.util.Set;

public final class BlockStateProviderInjector {
    private BlockStateProviderInjector() {}

    public static void init() {
        Object simple = BlockStateProviderAdapter.SIMPLE;
        Object weighted = BlockStateProviderAdapter.WEIGHTED;
        Object rotated = BlockStateProviderAdapter.ROTATED;
        Object randomizedInt = BlockStateProviderAdapter.RANDOMIZED_INT;
        MappedRegistryProxy.INSTANCE.setFrozen(BuiltInRegistriesProxy.BLOCKSTATE_PROVIDER_TYPE, false);
        register(Key.ce("simple_state_provider"), simple);
        register(Key.ce("weighted_state_provider"), weighted);
        register(Key.ce("rotated_block_provider"), rotated);
        register(Key.ce("randomized_int_state_provider"), randomizedInt);
        MappedRegistryProxy.INSTANCE.setFrozen(BuiltInRegistriesProxy.BLOCKSTATE_PROVIDER_TYPE, true);
    }

    private static void register(Key id, Object type) {
        Object identifier = KeyUtils.toIdentifier(id);
        Object holder = RegistryProxy.INSTANCE.registerForHolder$1(BuiltInRegistriesProxy.BLOCKSTATE_PROVIDER_TYPE, identifier, type);
        HolderProxy.ReferenceProxy.INSTANCE.bindValue(holder, type);
        HolderProxy.ReferenceProxy.INSTANCE.setTags(holder, Set.of());
    }
}
