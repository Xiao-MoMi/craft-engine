package net.momirealms.craftengine.proxy.minecraft.core.particles;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.core.particles.ShriekParticleOption")
public interface ShriekParticleOptionProxy {
    ShriekParticleOptionProxy INSTANCE = ASMProxyFactory.create(ShriekParticleOptionProxy.class);

    @ConstructorInvoker
    Object newInstance(int delay);
}
