package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBuiltInRegistries;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MRegistries;
import net.momirealms.craftengine.proxy.minecraft.core.RegistryAccessProxy;
import net.momirealms.craftengine.proxy.minecraft.server.MinecraftServerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlockProxy;

public final class RegistryUtils {

    private RegistryUtils() {}

    public static int currentBlockRegistrySize() {
        try {
            return (int) CoreReflections.method$IdMapper$size.invoke(BlockProxy.BLOCK_STATE_REGISTRY);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static int currentBiomeRegistrySize() {
        try {
            return (int) CoreReflections.method$IdMap$size.invoke(RegistryAccessProxy.INSTANCE.registryOrThrow(MinecraftServerProxy.INSTANCE.registryAccess(MinecraftServerProxy.INSTANCE.getServer()), MRegistries.BIOME));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static int currentEntityTypeRegistrySize() {
        try {
            return (int) CoreReflections.method$IdMap$size.invoke(MBuiltInRegistries.ENTITY_TYPE);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
