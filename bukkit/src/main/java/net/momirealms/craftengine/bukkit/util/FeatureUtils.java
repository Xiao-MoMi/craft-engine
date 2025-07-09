package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MRegistries;
import net.momirealms.craftengine.core.util.Key;

public class FeatureUtils {

    private FeatureUtils() {}

    public static Object createConfiguredFeatureKey(Key id) {
        return FastNMS.INSTANCE.method$ResourceKey$create(MRegistries.CONFIGURED_FEATURE, KeyUtils.toResourceLocation(id));
    }

    public static Object createPlacedFeatureKey(Key id) {
        return FastNMS.INSTANCE.method$ResourceKey$create(MRegistries.PLACED_FEATURE, KeyUtils.toResourceLocation(id));
    }
}
