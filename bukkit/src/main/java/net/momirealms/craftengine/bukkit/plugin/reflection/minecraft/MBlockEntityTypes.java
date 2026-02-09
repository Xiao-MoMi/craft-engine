package net.momirealms.craftengine.bukkit.plugin.reflection.minecraft;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.core.RegistryProxy;

public final class MBlockEntityTypes {
    private MBlockEntityTypes() {}

    // 1.21.9+
    public static final Object SHELF ;
    public static final int SHELF$registryId;

    private static Object getById(String id) {
        Object rl = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", id);
        return FastNMS.INSTANCE.method$Registry$getValue(MBuiltInRegistries.BLOCK_ENTITY_TYPE, rl);
    }

    private static int getRegistryId(Object type) {
        if (type == null) return -1;
        return RegistryProxy.INSTANCE.getId(MBuiltInRegistries.BLOCK_ENTITY_TYPE, type);
    }

    static {
        SHELF = MiscUtils.requireNonNullIf(getById("shelf"), VersionHelper.isOrAbove1_21_9());
        SHELF$registryId = getRegistryId("shelf"); // fixme 有问题，怎么是-1
    }
}
