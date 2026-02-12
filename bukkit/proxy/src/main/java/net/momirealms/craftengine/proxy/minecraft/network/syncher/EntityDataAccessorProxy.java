package net.momirealms.craftengine.proxy.minecraft.network.syncher;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.network.syncher.EntityDataAccessor")
public interface EntityDataAccessorProxy {
    EntityDataAccessorProxy INSTANCE = ASMProxyFactory.create(EntityDataAccessorProxy.class);

    @ConstructorInvoker
    Object newInstance(int id, @Type(clazz = EntityDataSerializerProxy.class) Object serializer);
}
