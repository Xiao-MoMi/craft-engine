package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.core.*;
import net.momirealms.craftengine.proxy.minecraft.core.registries.BuiltInRegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.core.registries.RegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.server.MinecraftServerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlockProxy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public final class RegistryUtils {
    public static final Object CONFIGURED_FEATURE = VersionHelper.isOrAbove26_3
            ? RegistriesProxy.INSTANCE.getConfiguredFeature$1()
            : RegistriesProxy.INSTANCE.getConfiguredFeature$0();

    private RegistryUtils() {}

    public static void registerAndFreeze(Object registry, Runnable registration) {
        MappedRegistryProxy.INSTANCE.setFrozen(registry, false);
        try {
            registration.run();
        } finally {
            MappedRegistryProxy.INSTANCE.setFrozen(registry, true);
        }
    }

    public static void registerBoundValue(Object registry, Key id, Object value) {
        Object holder = RegistryProxy.INSTANCE.registerForHolder$1(registry, KeyUtils.toIdentifier(id), value);
        HolderProxy.ReferenceProxy.INSTANCE.bindValue(holder, value);
        HolderProxy.ReferenceProxy.INSTANCE.setTags(holder, Set.of());
    }

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
