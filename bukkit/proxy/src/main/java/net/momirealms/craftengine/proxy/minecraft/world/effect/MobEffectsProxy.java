package net.momirealms.craftengine.proxy.minecraft.world.effect;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.effect.MobEffects")
public interface MobEffectsProxy {
    MobEffectsProxy INSTANCE = ASMProxyFactory.create(MobEffectsProxy.class);
    Object MINING_FATIGUE = INSTANCE.getMiningFatigue();
    Object HASTE = INSTANCE.getHaste();

    @FieldGetter(name = {"MINING_FATIGUE", "DIG_SLOWDOWN"}, isStatic = true)
    Object getMiningFatigue();

    @FieldGetter(name = {"HASTE", "DIG_SPEED"}, isStatic = true)
    Object getHaste();
}
