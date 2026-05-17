package net.momirealms.craftengine.proxy.minecraft.server.network.config;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.server.network.config.JoinWorldTask", activeIf = "min_version=1.20.2")
public interface JoinWorldTaskProxy {
    JoinWorldTaskProxy INSTANCE = ASMProxyFactory.create(JoinWorldTaskProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.server.network.config.JoinWorldTask");
    Object TYPE = INSTANCE != null ? INSTANCE.getType() : null;

    @ConstructorInvoker
    Object newInstance();

    @FieldGetter(name = "TYPE", isStatic = true)
    Object getType();
}
