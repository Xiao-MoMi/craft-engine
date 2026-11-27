package net.momirealms.craftengine.proxy.bukkit.craftbukkit.projectiles;

import net.momirealms.craftengine.proxy.minecraft.world.level.block.entity.DispenserBlockEntityProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;
import org.bukkit.projectiles.ProjectileSource;

@ReflectionProxy(name = "org.bukkit.craftbukkit.projectiles.CraftBlockProjectileSource")
public interface CraftBlockProjectileSourceProxy {
    CraftBlockProjectileSourceProxy INSTANCE = ASMProxyFactory.create(CraftBlockProjectileSourceProxy.class);

    @ConstructorInvoker
    ProjectileSource newInstance(@Type(clazz = DispenserBlockEntityProxy.class) Object dispenser);
}
