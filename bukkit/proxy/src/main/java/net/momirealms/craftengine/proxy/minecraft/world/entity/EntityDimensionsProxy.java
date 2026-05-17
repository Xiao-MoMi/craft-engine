package net.momirealms.craftengine.proxy.minecraft.world.entity;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.entity.EntityDimensions")
public interface EntityDimensionsProxy {
    EntityDimensionsProxy INSTANCE = ASMProxyFactory.create(EntityDimensionsProxy.class);

    @FieldGetter(name = "width")
    float getWidth(Object target);

    @FieldGetter(name = "height")
    float getHeight(Object target);

    @FieldGetter(name = "fixed")
    boolean isFixed(Object target);
}
