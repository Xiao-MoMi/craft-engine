package net.momirealms.craftengine.proxy.minecraft.core.particles;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import org.joml.Vector3f;

@ReflectionProxy(name = "net.minecraft.core.particles.DustParticleOptions")
public interface DustParticleOptionsProxy {
    DustParticleOptionsProxy INSTANCE = ASMProxyFactory.create(DustParticleOptionsProxy.class);

    @ConstructorInvoker(activeIf = "max_version=1.21.1")
    Object newInstance(Vector3f color, float scale);

    @ConstructorInvoker(activeIf = "min_version=1.21.2")
    Object newInstance(int color, float scale);
}
