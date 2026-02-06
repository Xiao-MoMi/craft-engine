package net.momirealms.craftengine.proxy.minecraft.network.config;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.server.network.config.JoinWorldTask")
public interface JoinWorldTaskProxy {
    JoinWorldTaskProxy INSTANCE = ASMProxyFactory.create(JoinWorldTaskProxy.class);
    Object TYPE = INSTANCE.getType();

    @ConstructorInvoker
    Object newInstance();

    @FieldGetter(name = "TYPE", isStatic = true)
    Object getType();
}
