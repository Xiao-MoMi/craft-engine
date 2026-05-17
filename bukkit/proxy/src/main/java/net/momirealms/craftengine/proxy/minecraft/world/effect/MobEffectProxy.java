package net.momirealms.craftengine.proxy.minecraft.world.effect;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.effect.MobEffect")
public interface MobEffectProxy {
    MobEffectProxy INSTANCE = ASMProxyFactory.create(MobEffectProxy.class);
}
