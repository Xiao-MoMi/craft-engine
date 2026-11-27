package net.momirealms.craftengine.proxy.minecraft.world.entity.projectile;

import net.momirealms.craftengine.proxy.minecraft.server.level.ServerLevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.entity.projectile.Projectile")
public interface ProjectileProxy extends EntityProxy {
    ProjectileProxy INSTANCE = ASMProxyFactory.create(ProjectileProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.entity.projectile.Projectile");

    @MethodInvoker(name = "shoot")
    void shoot(Object target, double x, double y, double z, float power, float uncertainty);

    @MethodInvoker(name = "spawnProjectileUsingShootDelayed", isStatic = true, activeIf = "has_patch=paper && min_version=1.21.2")
    Object spawnProjectileUsingShootDelayed(@Type(clazz = ProjectileProxy.class) Object projectile,
                                           @Type(clazz = ServerLevelProxy.class) Object level,
                                           @Type(clazz = ItemStackProxy.class) Object stack,
                                           double x, double y, double z, float power, float uncertainty);

    @MethodInvoker(name = "spawnProjectile", isStatic = true, activeIf = "!has_patch=paper && min_version=1.21.2")
    Object spawnProjectile(@Type(clazz = ProjectileProxy.class) Object projectile,
                           @Type(clazz = ServerLevelProxy.class) Object level,
                           @Type(clazz = ItemStackProxy.class) Object stack);

    @ReflectionProxy(name = "net.minecraft.world.entity.projectile.Projectile$Delayed", activeIf = "has_patch=paper && min_version=1.21.2")
    interface DelayedProxy {
        DelayedProxy INSTANCE = ASMProxyFactory.create(DelayedProxy.class);

        @MethodInvoker(name = "spawn")
        Object spawn(Object target);
    }
}
