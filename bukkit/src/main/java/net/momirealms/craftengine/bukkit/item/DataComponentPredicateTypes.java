package net.momirealms.craftengine.bukkit.item;


import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.bukkit.util.RegistryUtils;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.core.registries.BuiltInRegistriesProxy;

public final class DataComponentPredicateTypes {
    private DataComponentPredicateTypes() {}

    public static Object byId(Key key) {
        if (!VersionHelper.isOrAbove1_21_5()) return null;
        return RegistryUtils.getRegistryValue(BuiltInRegistriesProxy.DATA_COMPONENT_PREDICATE_TYPE, KeyUtils.toIdentifier(key));
    }
}
