package net.momirealms.craftengine.proxy.minecraft.world.phys;

import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.core.DirectionProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.phys.BlockHitResult")
public interface BlockHitResultProxy extends HitResultProxy {
    BlockHitResultProxy INSTANCE = ASMProxyFactory.create(BlockHitResultProxy.class);

    @ConstructorInvoker
    Object newInstance(
            @Type(clazz = Vec3Proxy.class) Object pos,
            @Type(clazz = DirectionProxy.class) Object side,
            @Type(clazz = BlockPosProxy.class) Object blockPos,
            boolean insideBlock
    );

    @FieldGetter(name = "direction")
    Object getDirection(Object target);

    @FieldGetter(name = "blockPos")
    Object getBlockPos(Object target);

    @FieldGetter(name = "miss")
    boolean isMiss(Object target);

    @FieldGetter(name = "inside")
    boolean isInside(Object target);
}
