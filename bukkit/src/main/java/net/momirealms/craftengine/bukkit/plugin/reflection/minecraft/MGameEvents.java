package net.momirealms.craftengine.bukkit.plugin.reflection.minecraft;

import net.momirealms.craftengine.bukkit.util.RegistryUtils;
import net.momirealms.craftengine.proxy.minecraft.core.RegistryProxy;
import net.momirealms.craftengine.proxy.minecraft.resources.IdentifierProxy;

public final class MGameEvents {
    private MGameEvents() {}

    public static final Object BLOCK_ACTIVATE = getById("block_activate");
    public static final Object BLOCK_ACTIVATE$holder = getHolderById("block_activate");
    public static final Object BLOCK_DEACTIVATE = getById("block_deactivate");
    public static final Object BLOCK_DEACTIVATE$holder = getHolderById("block_deactivate");

    private static Object getById(String path) {
        Object id = IdentifierProxy.INSTANCE.newInstance("minecraft", path);
        return RegistryUtils.getRegistryValue(MBuiltInRegistries.GAME_EVENT, id);
    }

    private static Object getHolderById(String path) {
        Object id = IdentifierProxy.INSTANCE.newInstance("minecraft", path);
        return RegistryProxy.INSTANCE.get$0(MBuiltInRegistries.GAME_EVENT, id).orElseThrow();
    }
}
