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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RegistryUtils {
    private RegistryUtils() {}

    public static int currentBlockRegistrySize() {
        return IdMapperProxy.INSTANCE.size(BlockProxy.BLOCK_STATE_REGISTRY);
    }

    public static int currentBiomeRegistrySize() {
        return IdMapProxy.INSTANCE.size(lookupOrThrow(RegistriesProxy.BIOME));
    }

    public static int currentEntityTypeRegistrySize() {
        return IdMapProxy.INSTANCE.size(BuiltInRegistriesProxy.ENTITY_TYPE);
    }

    public static Object getRegistryAccess() {
        return MinecraftServerProxy.INSTANCE.registryAccess(MinecraftServerProxy.INSTANCE.getServer());
    }

    public static Object getRegistryValue(Object registry, Object id) {
        if (VersionHelper.isOrAbove1_21_2) {
            return RegistryProxy.INSTANCE.getValue(registry, id);
        } else {
            return RegistryProxy.INSTANCE.get$2(registry, id);
        }
    }

    @NotNull
    public static Object lookupOrThrow(Object registryKey) {
        return RegistryAccessProxy.INSTANCE.lookupOrThrow(getRegistryAccess(), registryKey);
    }

    @Nullable
    public static Object getHolder(Object registry, Object resourceKey) {
        if (VersionHelper.isOrAbove1_21_2) {
            return RegistryProxy.INSTANCE.get$1(registry, resourceKey).orElse(null);
        } else {
            return RegistryProxy.INSTANCE.getHolder$1(registry, resourceKey).orElse(null);
        }
    }

    @Nullable
    public static Object getHolderById(Object registry, Object id) {
        if (VersionHelper.isOrAbove1_21_2) {
            return RegistryProxy.INSTANCE.get$0(registry, id).orElse(null);
        } else if (VersionHelper.isOrAbove1_20_5) {
            return RegistryProxy.INSTANCE.getHolder$0(registry, id).orElse(null);
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
