package net.momirealms.craftengine.proxy.minecraft.core.particles;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.core.particles.SculkChargeParticleOptions")
public interface SculkChargeParticleOptionsProxy {
    SculkChargeParticleOptionsProxy INSTANCE = ASMProxyFactory.create(SculkChargeParticleOptionsProxy.class);

    @ConstructorInvoker
    Object newInstance(float roll);
}
