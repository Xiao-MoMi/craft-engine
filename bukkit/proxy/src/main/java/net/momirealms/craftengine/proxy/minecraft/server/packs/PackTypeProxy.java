package net.momirealms.craftengine.proxy.minecraft.server.packs;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.server.packs.PackType")
public interface PackTypeProxy {
    PackTypeProxy INSTANCE = ASMProxyFactory.create(PackTypeProxy.class);
    Object[] VALUES = INSTANCE.values();
    Object CLIENT_RESOURCES = VALUES[0];
    Object SERVER_DATA = VALUES[1];

    @MethodInvoker(name = "values", isStatic = true)
    Object[] values();
}
