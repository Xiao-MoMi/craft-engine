package net.momirealms.craftengine.proxy.minecraft.core.particles;

import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.core.particles.TrailParticleOption", activeIf = "min_version=1.21.4")
public interface TrailParticleOptionProxy {
    TrailParticleOptionProxy INSTANCE = ASMProxyFactory.create(TrailParticleOptionProxy.class);

    @ConstructorInvoker
    Object newInstance(@Type(clazz = Vec3Proxy.class) Object target, int color, int duration);
}
