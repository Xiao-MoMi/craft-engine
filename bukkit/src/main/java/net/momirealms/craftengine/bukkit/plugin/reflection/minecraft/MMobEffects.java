package net.momirealms.craftengine.bukkit.plugin.reflection.minecraft;

import net.momirealms.craftengine.bukkit.util.RegistryUtils;
import net.momirealms.craftengine.proxy.minecraft.resources.IdentifierProxy;

public final class MMobEffects {
    private MMobEffects() {}

    // for 1.20.1-1.20.4
    public static final Object MINING_FATIGUE = getById("mining_fatigue");
    public static final Object HASTE = getById("haste");
    public static final Object INVISIBILITY = getById("invisibility");

    private static Object getById(String path) {
        Object id = IdentifierProxy.INSTANCE.newInstance("minecraft", path);
        return RegistryUtils.getRegistryValue(MBuiltInRegistries.MOB_EFFECT, id);
    }
}
