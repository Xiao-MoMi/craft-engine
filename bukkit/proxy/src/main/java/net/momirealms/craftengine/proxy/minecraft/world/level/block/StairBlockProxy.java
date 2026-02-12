package net.momirealms.craftengine.proxy.minecraft.world.level.block;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.block.StairBlock")
public interface StairBlockProxy extends BlockProxy {
    StairBlockProxy INSTANCE = ASMProxyFactory.create(StairBlockProxy.class);
    Object FACING = INSTANCE.getFacingProperty();
    Object HALF = INSTANCE.getHalfProperty();
    Object SHAPE = INSTANCE.getShapeProperty();

    @FieldGetter(name = "FACING", isStatic = true)
    Object getFacingProperty();

    @FieldGetter(name = "HALF", isStatic = true)
    Object getHalfProperty();

    @FieldGetter(name = "SHAPE", isStatic = true)
    Object getShapeProperty();
}
