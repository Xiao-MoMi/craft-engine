package net.momirealms.craftengine.proxy.minecraft.world.entity;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.entity.Pose")
public interface PoseProxy {
    PoseProxy INSTANCE = ASMProxyFactory.create(PoseProxy.class);
}
