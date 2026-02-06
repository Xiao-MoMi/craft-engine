package net.momirealms.craftengine.proxy.minecraft.world.phys;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.phys.BlockHitResult")
public interface BlockHitResultProxy extends HitResultProxy {
    BlockHitResultProxy INSTANCE = ASMProxyFactory.create(BlockHitResultProxy.class);

    @FieldGetter(name = "inside")
    boolean isInside(Object target);
}
