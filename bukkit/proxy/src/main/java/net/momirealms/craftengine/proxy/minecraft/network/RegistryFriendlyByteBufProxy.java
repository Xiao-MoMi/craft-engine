package net.momirealms.craftengine.proxy.minecraft.network;

import io.netty.buffer.ByteBuf;
import net.momirealms.craftengine.proxy.minecraft.core.RegistryAccessProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.network.RegistryFriendlyByteBuf", activeIf = "min_version=1.20.5")
public interface RegistryFriendlyByteBufProxy extends FriendlyByteBufProxy {
    RegistryFriendlyByteBufProxy INSTANCE = ASMProxyFactory.create(RegistryFriendlyByteBufProxy.class);

    @ConstructorInvoker
    ByteBuf newInstance(ByteBuf source, @Type(clazz = RegistryAccessProxy.class) Object registryAccess);
}
