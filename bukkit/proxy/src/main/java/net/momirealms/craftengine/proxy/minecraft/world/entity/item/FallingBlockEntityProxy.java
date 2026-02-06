package net.momirealms.craftengine.proxy.minecraft.world.entity.item;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.entity.item.FallingBlockEntity")
public interface FallingBlockEntityProxy {
    FallingBlockEntityProxy INSTANCE = ASMProxyFactory.create(FallingBlockEntityProxy.class);

    @FieldGetter(name = "blockState")
    Object getBlockState(Object target);

    @MethodInvoker(name = "setHurtsEntities")
    void setHurtsEntities(Object target, float fallDamagePerDistance, int fallDamageMax);
}
