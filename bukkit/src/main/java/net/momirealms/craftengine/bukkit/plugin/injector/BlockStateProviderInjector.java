package net.momirealms.craftengine.bukkit.plugin.injector;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBuiltInRegistries;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.proxy.minecraft.core.HolderProxy;
import net.momirealms.craftengine.proxy.minecraft.core.MappedRegistryProxy;
import net.momirealms.craftengine.proxy.minecraft.core.RegistryProxy;

import java.util.Set;

public final class BlockStateProviderInjector {
    private BlockStateProviderInjector() {}

    public static void init() throws ReflectiveOperationException {
        MappedRegistryProxy.INSTANCE.setFrozen(MBuiltInRegistries.BLOCKSTATE_PROVIDER_TYPE, false);
        register(Key.ce("simple_state_provider"), FastNMS.INSTANCE.getCraftEngineCustomSimpleStateProviderType());
        register(Key.ce("weighted_state_provider"), FastNMS.INSTANCE.getCraftEngineCustomWeightedStateProviderType());
        register(Key.ce("rotated_block_provider"), FastNMS.INSTANCE.getCraftEngineCustomRotatedBlockProviderType());
        register(Key.ce("randomized_int_state_provider"), FastNMS.INSTANCE.getCraftEngineCustomRandomizedIntStateProviderType());
        MappedRegistryProxy.INSTANCE.setFrozen(MBuiltInRegistries.BLOCKSTATE_PROVIDER_TYPE, true);
    }

    private static void register(Key id, Object type) {
        Object identifier = KeyUtils.toIdentifier(id);
        Object holder = RegistryProxy.INSTANCE.registerForHolder$1(MBuiltInRegistries.BLOCKSTATE_PROVIDER_TYPE, identifier, type);
        HolderProxy.ReferenceProxy.INSTANCE.bindValue(holder, type);
        HolderProxy.ReferenceProxy.INSTANCE.setTags(holder, Set.of());
    }
}
