package net.momirealms.craftengine.proxy.minecraft.world.effect;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.effect.MobEffectInstance")
public interface MobEffectInstanceProxy {
    MobEffectInstanceProxy INSTANCE = ASMProxyFactory.create(MobEffectInstanceProxy.class);


}
