package net.momirealms.craftengine.proxy.minecraft.world.effect;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.effect.MobEffects")
public interface MobEffectsProxy {
    MobEffectsProxy INSTANCE = ASMProxyFactory.create(MobEffectsProxy.class);


}
