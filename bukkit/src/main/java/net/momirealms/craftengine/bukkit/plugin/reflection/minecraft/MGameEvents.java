package net.momirealms.craftengine.bukkit.plugin.reflection.minecraft;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.proxy.minecraft.core.RegistryProxy;

public final class MGameEvents {
    private MGameEvents() {}

    public static final Object BLOCK_ACTIVATE = getById("block_activate");
    public static final Object BLOCK_ACTIVATE$holder = getHolderById("block_activate");
    public static final Object BLOCK_DEACTIVATE = getById("block_deactivate");
    public static final Object BLOCK_DEACTIVATE$holder = getHolderById("block_deactivate");

    private static Object getById(String id) {
        Object rl = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", id);
        return FastNMS.INSTANCE.method$Registry$getValue(MBuiltInRegistries.GAME_EVENT, rl);
    }

    private static Object getHolderById(String id) {
        Object rl = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", id);
        return RegistryProxy.INSTANCE.get$0(MBuiltInRegistries.GAME_EVENT, rl).orElseThrow();
    }
}
