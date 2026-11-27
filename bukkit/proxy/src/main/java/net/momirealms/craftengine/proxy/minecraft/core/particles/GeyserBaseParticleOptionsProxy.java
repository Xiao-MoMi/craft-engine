package net.momirealms.craftengine.proxy.minecraft.core.particles;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.core.particles.GeyserBaseParticleOptions", activeIf = "min_version=26.2")
public interface GeyserBaseParticleOptionsProxy {
    GeyserBaseParticleOptionsProxy INSTANCE = ASMProxyFactory.create(GeyserBaseParticleOptionsProxy.class);

    @ConstructorInvoker
    Object newInstance(@Type(clazz = ParticleTypeProxy.class) Object type, int waterBlocks, float burstImpulse);
}
