package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MRegistries;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.proxy.minecraft.resources.ResourceKeyProxy;

public final class FeatureUtils {

    private FeatureUtils() {}

    public static Object createConfiguredFeatureKey(Key id) {
        return ResourceKeyProxy.INSTANCE.create(MRegistries.CONFIGURED_FEATURE, KeyUtils.toIdentifier(id));
    }

    public static Object createPlacedFeatureKey(Key id) {
        return ResourceKeyProxy.INSTANCE.create(MRegistries.PLACED_FEATURE, KeyUtils.toIdentifier(id));
    }
}
