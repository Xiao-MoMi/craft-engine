package net.momirealms.craftengine.bukkit.plugin.reflection.minecraft;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.util.RegistryUtils;
import net.momirealms.craftengine.proxy.minecraft.core.RegistryProxy;
import net.momirealms.craftengine.proxy.minecraft.resources.IdentifierProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidProxy;

public final class MFluids {
    private MFluids() {}

    public static final Object WATER = getById("water");
    public static final Object WATER$defaultState = FluidProxy.INSTANCE.getDefaultFluidState(WATER);
    public static final Object FLOWING_WATER = getById("flowing_water");
    public static final Object LAVA = getById("lava");
    public static final Object FLOWING_LAVA = getById("flowing_lava");
    public static final Object EMPTY = getById("empty");
    public static final Object EMPTY$defaultState = FluidProxy.INSTANCE.getDefaultFluidState(EMPTY);

    private static Object getById(String path) {
        Object id = IdentifierProxy.INSTANCE.newInstance("minecraft", path);
        return RegistryUtils.getRegistryValue(MBuiltInRegistries.FLUID, id);
    }
}
