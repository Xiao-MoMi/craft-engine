package net.momirealms.craftengine.bukkit.plugin.reflection.minecraft;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.ReflectionInitException;

public final class MFluids {
    private MFluids() {}

    public static final Object WATER;
    public static final Object WATER$defaultState;
    public static final Object FLOWING_WATER;
    public static final Object LAVA;
    public static final Object FLOWING_LAVA;
    public static final Object EMPTY;
    public static final Object EMPTY$defaultState;

    private static Object getById(String id) throws ReflectiveOperationException {
        Object rl = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", id);
        return FastNMS.INSTANCE.method$Registry$getValue(MBuiltInRegistries.FLUID, rl);
    }

    static {
        try {
            WATER = getById("water");
            WATER$defaultState = CoreReflections.method$Fluid$defaultFluidState.invoke(WATER);
            FLOWING_WATER = getById("flowing_water");
            LAVA = getById("lava");
            FLOWING_LAVA = getById("flowing_lava");
            EMPTY = getById("empty");
            EMPTY$defaultState = CoreReflections.method$Fluid$defaultFluidState.invoke(EMPTY);
        } catch (ReflectiveOperationException e) {
            throw new ReflectionInitException("Failed to init Fluids", e);
        }
    }
}
