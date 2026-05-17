package net.momirealms.craftengine.proxy.minecraft.world.entity.vehicle;

import net.momirealms.craftengine.proxy.minecraft.world.entity.LivingEntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.CollisionGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.AABBProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.entity.vehicle.DismountHelper")
public interface DismountHelperProxy {
    DismountHelperProxy INSTANCE = ASMProxyFactory.create(DismountHelperProxy.class);

    @MethodInvoker(name = "canDismountTo", isStatic = true)
    boolean canDismountTo(@Type(clazz = CollisionGetterProxy.class) Object level,
                          @Type(clazz = LivingEntityProxy.class) Object passenger,
                          @Type(clazz = AABBProxy.class) Object boundingBox);
}
