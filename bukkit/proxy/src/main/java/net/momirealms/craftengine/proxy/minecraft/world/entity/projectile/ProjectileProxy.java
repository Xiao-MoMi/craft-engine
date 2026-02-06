package net.momirealms.craftengine.proxy.minecraft.world.entity.projectile;

import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.entity.projectile.Projectile")
public interface ProjectileProxy extends EntityProxy {
    ProjectileProxy INSTANCE = ASMProxyFactory.create(ProjectileProxy.class);
}
