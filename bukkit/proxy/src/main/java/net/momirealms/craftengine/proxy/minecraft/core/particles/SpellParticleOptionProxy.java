package net.momirealms.craftengine.proxy.minecraft.core.particles;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.core.particles.SpellParticleOption", activeIf = "min_version=1.21.9")
public interface SpellParticleOptionProxy {
    SpellParticleOptionProxy INSTANCE = ASMProxyFactory.create(SpellParticleOptionProxy.class);

    @MethodInvoker(name = "create", isStatic = true)
    Object create(@Type(clazz = ParticleTypeProxy.class) Object type, int color, float power);
}
