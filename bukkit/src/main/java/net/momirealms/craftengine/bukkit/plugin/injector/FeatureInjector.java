package net.momirealms.craftengine.bukkit.plugin.injector;

import net.momirealms.craftengine.bukkit.util.RegistryUtils;
import net.momirealms.craftengine.bukkit.world.gen.SimpleBlockFeatureAdapter;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.proxy.minecraft.core.registries.BuiltInRegistriesProxy;

public final class FeatureInjector {
    private FeatureInjector() {
    }

    public static void init() {
        Object type = SimpleBlockFeatureAdapter.type();
        Object registry = BuiltInRegistriesProxy.FEATURE;
        RegistryUtils.registerAndFreeze(registry, () -> RegistryUtils.registerBoundValue(registry, Key.ce("simple_block"), type));
    }
}
