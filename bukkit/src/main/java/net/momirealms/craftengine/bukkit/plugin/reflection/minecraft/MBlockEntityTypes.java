package net.momirealms.craftengine.bukkit.plugin.reflection.minecraft;

import net.momirealms.craftengine.bukkit.util.RegistryUtils;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.core.RegistryProxy;
import net.momirealms.craftengine.proxy.minecraft.core.registries.BuiltInRegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.resources.IdentifierProxy;

public final class MBlockEntityTypes {
    private MBlockEntityTypes() {}

    // 1.21.9+
    public static final Object SHELF = MiscUtils.requireNonNullIf(getById("shelf"), VersionHelper.isOrAbove1_21_9());
    public static final int SHELF$registryId = getRegistryId("shelf"); // fixme 有问题，怎么是-1

    private static Object getById(String path) {
        Object id = IdentifierProxy.INSTANCE.newInstance("minecraft", path);
        return RegistryUtils.getRegistryValue(BuiltInRegistriesProxy.BLOCK_ENTITY_TYPE, id);
    }

    private static int getRegistryId(Object type) {
        if (type == null) return -1;
        return RegistryProxy.INSTANCE.getId$0(BuiltInRegistriesProxy.BLOCK_ENTITY_TYPE, type);
    }
}
