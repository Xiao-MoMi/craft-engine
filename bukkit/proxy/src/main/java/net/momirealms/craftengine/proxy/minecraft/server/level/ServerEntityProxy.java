package net.momirealms.craftengine.proxy.minecraft.server.level;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.server.level.ServerEntity")
public interface ServerEntityProxy {
    ServerEntityProxy INSTANCE = ASMProxyFactory.create(ServerEntityProxy.class);

    @FieldSetter(name = "updateInterval", activeIf = "max_version=26.2")
    default void setUpdateInterval(Object target, int value) {
        setUpdateIntervalValue(target, net.momirealms.craftengine.proxy.minecraft.world.entity.UpdateIntervalProxy.INSTANCE.periodic(value));
    }

    @FieldSetter(name = "updateInterval", activeIf = "min_version=26.3")
    void setUpdateIntervalValue(Object target, Object value);
}
