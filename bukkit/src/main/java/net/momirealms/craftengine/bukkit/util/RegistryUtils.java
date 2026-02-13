package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.core.IdMapProxy;
import net.momirealms.craftengine.proxy.minecraft.core.IdMapperProxy;
import net.momirealms.craftengine.proxy.minecraft.core.RegistryAccessProxy;
import net.momirealms.craftengine.proxy.minecraft.core.RegistryProxy;
import net.momirealms.craftengine.proxy.minecraft.core.registries.BuiltInRegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.core.registries.RegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.server.MinecraftServerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlockProxy;

public final class RegistryUtils {

    private RegistryUtils() {}

    public static int currentBlockRegistrySize() {
        return IdMapperProxy.INSTANCE.size(BlockProxy.BLOCK_STATE_REGISTRY);
    }

    public static int currentBiomeRegistrySize() {
        return IdMapProxy.INSTANCE.size(RegistryAccessProxy.INSTANCE.lookupOrThrow(getRegistryAccess(), RegistriesProxy.BIOME));
    }

    public static int currentEntityTypeRegistrySize() {
        return IdMapProxy.INSTANCE.size(BuiltInRegistriesProxy.ENTITY_TYPE);
    }

    public static Object getRegistryAccess() {
        return MinecraftServerProxy.INSTANCE.registryAccess(getServer());
    }

    public static Object getServer() {
        return MinecraftServerProxy.INSTANCE.getServer();
    }

    public static Object getRegistryValue(Object registry, Object id) {
        if (VersionHelper.isOrAbove1_21_2()) {
            return RegistryProxy.INSTANCE.getValue(registry, id);
        } else {
            return RegistryProxy.INSTANCE.get$2(registry, id);
        }
    }
}
