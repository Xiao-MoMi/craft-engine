package net.momirealms.craftengine.proxy.minecraft.core.particles;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.core.particles.ParticleType")
public interface ParticleTypeProxy {
    ParticleTypeProxy INSTANCE = ASMProxyFactory.create(ParticleTypeProxy.class);

    @MethodInvoker(name = "getDeserializer", activeIf = "max_version=1.20.4")
    Object getDeserializer(Object target);
}
