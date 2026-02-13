package net.momirealms.craftengine.proxy.minecraft.world.level.material;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.material.Fluid")
public interface FluidProxy {
    FluidProxy INSTANCE = ASMProxyFactory.create(FluidProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.material.Fluid");

    @FieldGetter(name = "defaultFluidState")
    Object getDefaultFluidState(Object target);
}
