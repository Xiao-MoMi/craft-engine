package net.momirealms.craftengine.bukkit.plugin.injector;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBuiltInRegistries;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.util.Key;

import java.util.Set;

public final class BlockStateProviderInjector {

    private BlockStateProviderInjector() {}

    public static void init() throws ReflectiveOperationException {
        CoreReflections.field$MappedRegistry$frozen.set(MBuiltInRegistries.BLOCKSTATE_PROVIDER_TYPE, false);

        register(Key.ce("simple_state_provider"), FastNMS.INSTANCE.getCraftEngineCustomSimpleStateProviderType());
        register(Key.ce("weighted_state_provider"), FastNMS.INSTANCE.getCraftEngineCustomWeightedStateProviderType());
        register(Key.ce("rotated_block_provider"), FastNMS.INSTANCE.getCraftEngineCustomRotatedBlockProviderType());
        register(Key.ce("randomized_int_state_provider"), FastNMS.INSTANCE.getCraftEngineCustomRandomizedIntStateProviderType());

        CoreReflections.field$MappedRegistry$frozen.set(MBuiltInRegistries.BLOCKSTATE_PROVIDER_TYPE, true);
    }

    private static void register(Key id, Object type) throws ReflectiveOperationException {
        Object resourceLocation = KeyUtils.toIdentifier(id);
        Object holder = CoreReflections.method$Registry$registerForHolder.invoke(null, MBuiltInRegistries.BLOCKSTATE_PROVIDER_TYPE, resourceLocation, type);
        CoreReflections.method$Holder$Reference$bindValue.invoke(holder, type);
        CoreReflections.field$Holder$Reference$tags.set(holder, Set.of());
    }
}
