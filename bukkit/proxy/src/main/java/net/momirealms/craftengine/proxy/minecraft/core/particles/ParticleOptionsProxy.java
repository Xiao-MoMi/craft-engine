package net.momirealms.craftengine.proxy.minecraft.core.particles;

import net.momirealms.craftengine.proxy.minecraft.network.FriendlyByteBufProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.core.particles.ParticleOptions")
public interface ParticleOptionsProxy {
    ParticleOptionsProxy INSTANCE = ASMProxyFactory.create(ParticleOptionsProxy.class);

    @MethodInvoker(name = "getType")
    Object getType(Object target);

    @MethodInvoker(name = "writeToNetwork", activeIf = "max_version=1.20.4")
    void writeToNetwork(Object target, @Type(clazz = FriendlyByteBufProxy.class) Object buf);

    @ReflectionProxy(name = "net.minecraft.core.particles.ParticleOptions$Deserializer", activeIf = "max_version=1.20.4")
    interface DeserializerProxy {
        DeserializerProxy INSTANCE = ASMProxyFactory.create(DeserializerProxy.class);

        @MethodInvoker(name = "fromNetwork")
        <T> T fromNetwork(Object target, @Type(clazz = ParticleTypeProxy.class) Object type, @Type(clazz = FriendlyByteBufProxy.class) Object buf);
    }
}
