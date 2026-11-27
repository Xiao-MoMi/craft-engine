package net.momirealms.craftengine.proxy.minecraft.network.protocol;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.protocol.PacketType", activeIf = "min_version=1.20.5")
public interface PacketTypeProxy {
    PacketTypeProxy INSTANCE = ASMProxyFactory.create(PacketTypeProxy.class);

    @MethodInvoker(name = "id")
    Object id(Object target);
}
