package net.momirealms.craftengine.proxy.minecraft.world.entity.projectile;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = {"net.minecraft.world.entity.projectile.AbstractArrow", "net.minecraft.world.entity.projectile.arrow.AbstractArrow"})
public interface AbstractArrowProxy extends ProjectileProxy {
    AbstractArrowProxy INSTANCE = ASMProxyFactory.create(AbstractArrowProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.entity.projectile.AbstractArrow", "net.minecraft.world.entity.projectile.arrow.AbstractArrow");
}
