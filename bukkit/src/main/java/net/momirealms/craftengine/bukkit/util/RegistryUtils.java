package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBuiltInRegistries;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MRegistries;

public final class RegistryUtils {

    private RegistryUtils() {}

    public static int currentBlockRegistrySize() {
        try {
            return (int) CoreReflections.method$IdMapper$size.invoke(CoreReflections.instance$Block$BLOCK_STATE_REGISTRY);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static int currentBiomeRegistrySize() {
        try {
            Object idMap = CoreReflections.method$Registry$asHolderIdMap.invoke(FastNMS.INSTANCE.method$RegistryAccess$lookupOrThrow(FastNMS.INSTANCE.registryAccess(), MRegistries.BIOME));
            return (int) CoreReflections.method$IdMap$size.invoke(idMap);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static int currentEntityTypeRegistrySize() {
        try {
            Object idMap = CoreReflections.method$Registry$asHolderIdMap.invoke(MBuiltInRegistries.ENTITY_TYPE);
            return (int) CoreReflections.method$IdMap$size.invoke(idMap);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
