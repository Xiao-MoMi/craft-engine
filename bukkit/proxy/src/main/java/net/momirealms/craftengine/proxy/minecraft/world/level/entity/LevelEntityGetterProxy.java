package net.momirealms.craftengine.proxy.minecraft.world.level.entity;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.entity.LevelEntityGetter")
public interface LevelEntityGetterProxy {
    LevelEntityGetterProxy INSTANCE = ASMProxyFactory.create(LevelEntityGetterProxy.class);

    @MethodInvoker(name = "getAll")
    Iterable<Object> getAll(Object target);
}
