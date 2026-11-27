package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.craftengine.proxy.minecraft.core.particles.ParticleOptionsProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket")
public interface ClientboundLevelParticlesPacketProxy {
    ClientboundLevelParticlesPacketProxy INSTANCE = ASMProxyFactory.create(ClientboundLevelParticlesPacketProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket");

    @ConstructorInvoker(activeIf = "max_version=1.21.3")
    Object newInstance$0(@Type(clazz = ParticleOptionsProxy.class) Object options,
                         boolean overrideLimiter,
                         double x,
                         double y,
                         double z,
                         float dx,
                         float dy,
                         float dz,
                         float speed,
                         int count);

    @ConstructorInvoker(activeIf = "min_version=1.21.4")
    Object newInstance$1(@Type(clazz = ParticleOptionsProxy.class) Object options,
                         boolean overrideLimiter,
                         boolean alwaysShow,
                         double x,
                         double y,
                         double z,
                         float dx,
                         float dy,
                         float dz,
                         float speed,
                         int count);
}