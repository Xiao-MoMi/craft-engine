package net.momirealms.craftengine.bukkit.util;


import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.proxy.minecraft.core.registries.RegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.resources.ResourceKeyProxy;

public final class FeatureUtils {

    private FeatureUtils() {}

    public static Object createConfiguredFeatureKey(Key id) {
        return ResourceKeyProxy.INSTANCE.create(RegistriesProxy.CONFIGURED_FEATURE, KeyUtils.toIdentifier(id));
    }

    public static Object createPlacedFeatureKey(Key id) {
        return ResourceKeyProxy.INSTANCE.create(RegistriesProxy.PLACED_FEATURE, KeyUtils.toIdentifier(id));
    }
}
