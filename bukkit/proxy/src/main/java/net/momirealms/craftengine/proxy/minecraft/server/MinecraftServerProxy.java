package net.momirealms.craftengine.proxy.minecraft.server;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.server.MinecraftServer")
public interface MinecraftServerProxy {
    MinecraftServerProxy INSTANCE = ASMProxyFactory.create(MinecraftServerProxy.class);

    @MethodInvoker(name = "getServer", isStatic = true)
    Object getServer();

    @MethodInvoker(name = "getPackRepository")
    Object getPackRepository(Object target);

    @FieldGetter(name = "connection")
    Object getConnection(Object target);
}
