package net.momirealms.craftengine.proxy.minecraft.server.level;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.server.level.ServerEntity")
public interface ServerEntityProxy {
    ServerEntityProxy INSTANCE = ASMProxyFactory.create(ServerEntityProxy.class);

    @FieldSetter(name = "updateInterval")
    void setUpdateInterval(Object target, int value);
}
