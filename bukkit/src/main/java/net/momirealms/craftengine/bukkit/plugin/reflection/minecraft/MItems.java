package net.momirealms.craftengine.bukkit.plugin.reflection.minecraft;

import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.bukkit.util.RegistryUtils;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.proxy.minecraft.core.registries.BuiltInRegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.resources.IdentifierProxy;
import org.jetbrains.annotations.Nullable;

public final class MItems {
    private MItems() {}

    public static final Object AIR = getById("air");
    public static final Object WATER_BUCKET = getById("water_bucket");
    public static final Object BARRIER = getById("barrier");
    public static final Object DEBUG_STICK = getById("debug_stick");

    @Nullable
    private static Object getById(String path) {
        Object id = IdentifierProxy.INSTANCE.newInstance("minecraft", path);
        return RegistryUtils.getRegistryValue(BuiltInRegistriesProxy.ITEM, id);
    }

    @Nullable
    public static Object getById(Key id) {
        return RegistryUtils.getRegistryValue(BuiltInRegistriesProxy.ITEM, KeyUtils.toIdentifier(id));
    }
}
