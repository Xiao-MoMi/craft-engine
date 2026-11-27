package net.momirealms.craftengine.bukkit.plugin.injector;

import net.momirealms.craftengine.bukkit.util.RegistryUtils;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.proxy.minecraft.core.registries.BuiltInRegistriesProxy;

public final class LootEntryInjector {
    private LootEntryInjector() {
    }

    public static void init() {
        Object type = CraftEngineItemGenerator.type();
        Object registry = BuiltInRegistriesProxy.LOOT_POOL_ENTRY_TYPE;
        RegistryUtils.registerAndFreeze(registry, () -> RegistryUtils.registerBoundValue(registry, Key.ce("item"), type));
    }
}
