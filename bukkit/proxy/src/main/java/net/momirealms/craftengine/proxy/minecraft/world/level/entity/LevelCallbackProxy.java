package net.momirealms.craftengine.proxy.minecraft.world.level.entity;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.entity.LevelCallback")
public interface LevelCallbackProxy {
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.entity.LevelCallback");
    LevelCallbackProxy INSTANCE = ASMProxyFactory.create(LevelCallbackProxy.class);

    @MethodInvoker(name = "onTickingEnd")
    void onTickingEnd(Object target, Object entity);
}
