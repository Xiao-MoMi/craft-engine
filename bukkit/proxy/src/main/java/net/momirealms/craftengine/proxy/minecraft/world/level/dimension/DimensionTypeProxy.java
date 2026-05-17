package net.momirealms.craftengine.proxy.minecraft.world.level.dimension;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.dimension.DimensionType")
public interface DimensionTypeProxy {
    DimensionTypeProxy INSTANCE = ASMProxyFactory.create(DimensionTypeProxy.class);

    @FieldGetter(name = "minY")
    int getMinY(Object target);
}
