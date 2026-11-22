package net.momirealms.craftengine.proxy.minecraft.core.particles;

import net.momirealms.craftengine.proxy.minecraft.world.level.gameevent.PositionSourceProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.core.particles.VibrationParticleOption")
public interface VibrationParticleOptionProxy {
    VibrationParticleOptionProxy INSTANCE = ASMProxyFactory.create(VibrationParticleOptionProxy.class);

    @ConstructorInvoker
    Object newInstance(@Type(clazz = PositionSourceProxy.class) Object destination, int arrivalTime);
}
