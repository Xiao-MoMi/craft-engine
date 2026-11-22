package net.momirealms.craftengine.proxy.minecraft.core.particles;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.core.particles.ColorParticleOption", activeIf = "min_version=1.20.5")
public interface ColorParticleOptionProxy {
    ColorParticleOptionProxy INSTANCE = ASMProxyFactory.create(ColorParticleOptionProxy.class);

    @MethodInvoker(name = "create", isStatic = true)
    Object create(@Type(clazz = ParticleTypeProxy.class) Object type, int color);
}
