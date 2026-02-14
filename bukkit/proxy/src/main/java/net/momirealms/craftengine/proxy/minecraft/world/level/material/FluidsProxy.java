package net.momirealms.craftengine.proxy.minecraft.world.level.material;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.material.Fluids")
public interface FluidsProxy {
    FluidsProxy INSTANCE = ASMProxyFactory.create(FluidsProxy.class);
    Object EMPTY = INSTANCE.getEmpty();
    Object FLOWING_WATER = INSTANCE.getFlowingWater();
    Object WATER = INSTANCE.getWater();
    Object FLOWING_LAVA = INSTANCE.getFlowingLava();
    Object LAVA = INSTANCE.getLava();
    Object EMPTY$defaultState = FluidProxy.INSTANCE.getDefaultFluidState(EMPTY);
    Object FLOWING_WATER$defaultState = FluidProxy.INSTANCE.getDefaultFluidState(FLOWING_WATER);
    Object WATER$defaultState = FluidProxy.INSTANCE.getDefaultFluidState(WATER);
    Object FLOWING_LAVA$defaultState = FluidProxy.INSTANCE.getDefaultFluidState(FLOWING_LAVA);
    Object LAVA$defaultState = FluidProxy.INSTANCE.getDefaultFluidState(LAVA);

    @FieldGetter(name = "EMPTY", isStatic = true)
    Object getEmpty();

    @FieldGetter(name = "FLOWING_WATER", isStatic = true)
    Object getFlowingWater();

    @FieldGetter(name = "WATER", isStatic = true)
    Object getWater();

    @FieldGetter(name = "FLOWING_LAVA", isStatic = true)
    Object getFlowingLava();

    @FieldGetter(name = "LAVA", isStatic = true)
    Object getLava();
}
