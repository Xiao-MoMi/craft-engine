package net.momirealms.craftengine.proxy.minecraft.world.entity;

import net.momirealms.craftengine.proxy.minecraft.core.HolderProxy;
import net.momirealms.craftengine.proxy.minecraft.world.effect.MobEffectProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.ai.attributes.AttributeProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.entity.LivingEntity")
public interface LivingEntityProxy extends EntityProxy {
    LivingEntityProxy INSTANCE = ASMProxyFactory.create(LivingEntityProxy.class);

    @MethodInvoker(name = "getLocalBoundsForPose")
    Object getLocalBoundsForPose(Object target, @Type(clazz = PoseProxy.class) Object pos);

    @MethodInvoker(name = "getAttribute", activeIf = "max_version=1.20.4")
    Object getAttribute$legacy(Object target, @Type(clazz = AttributeProxy.class) Object attribute);

    @MethodInvoker(name = "getAttribute", activeIf = "min_version=1.20.5")
    Object getAttribute(Object target, @Type(clazz = HolderProxy.class) Object attribute);

    @MethodInvoker(name = "getEffect", activeIf = "max_version=1.20.4")
    Object getEffect$legacy(Object target, @Type(clazz = MobEffectProxy.class) Object effect);

    @MethodInvoker(name = "getEffect", activeIf = "min_version=1.20.5")
    Object getEffect(Object target, @Type(clazz = HolderProxy.class) Object effect);
}
