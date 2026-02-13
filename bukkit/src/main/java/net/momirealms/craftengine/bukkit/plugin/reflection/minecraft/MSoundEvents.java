package net.momirealms.craftengine.bukkit.plugin.reflection.minecraft;

import net.momirealms.craftengine.bukkit.util.RegistryUtils;
import net.momirealms.craftengine.proxy.minecraft.core.registries.BuiltInRegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.resources.IdentifierProxy;

public final class MSoundEvents {
    private MSoundEvents() {}

    public static final Object EMPTY = getById("intentionally_empty");
    public static final Object TRIDENT_RIPTIDE_1 = getById("item.trident_riptide_1");
    public static final Object TRIDENT_RIPTIDE_2 = getById("item.trident_riptide_2");
    public static final Object TRIDENT_RIPTIDE_3 = getById("item.trident.riptide_3");
    public static final Object TRIDENT_THROW = getById("item.trident.throw");
    public static final Object TOTEM_USE = getById("item.totem.use");

    private static Object getById(String path) {
        Object id = IdentifierProxy.INSTANCE.newInstance("minecraft", path);
        return RegistryUtils.getRegistryValue(BuiltInRegistriesProxy.SOUND_EVENT, id);
    }
}
