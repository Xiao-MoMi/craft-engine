package net.momirealms.craftengine.proxy.minecraft.tags;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.tags.FluidTags")
public interface FluidTagsProxy {
    FluidTagsProxy INSTANCE = ASMProxyFactory.create(FluidTagsProxy.class);
    Object WATER = INSTANCE.getWater();
    Object LAVA = INSTANCE.getLava();

    @FieldGetter(name = "WATER", isStatic = true)
    Object getWater();

    @FieldGetter(name = "LAVA", isStatic = true)
    Object getLava();
}
