package net.momirealms.craftengine.proxy.minecraft.core.particles;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import org.joml.Vector3f;

@ReflectionProxy(name = "net.minecraft.core.particles.DustColorTransitionOptions")
public interface DustColorTransitionOptionsProxy {
    DustColorTransitionOptionsProxy INSTANCE = ASMProxyFactory.create(DustColorTransitionOptionsProxy.class);

    @ConstructorInvoker(activeIf = "max_version=1.21.1")
    Object newInstance(Vector3f from, Vector3f to, float scale);

    @ConstructorInvoker(activeIf = "min_version=1.21.2")
    Object newInstance(int from, int to, float scale);
}
