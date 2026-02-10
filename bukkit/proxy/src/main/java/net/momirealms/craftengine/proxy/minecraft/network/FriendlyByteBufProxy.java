package net.momirealms.craftengine.proxy.minecraft.network;

import io.netty.buffer.ByteBuf;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.FriendlyByteBuf")
public interface FriendlyByteBufProxy {
    FriendlyByteBufProxy INSTANCE = ASMProxyFactory.create(FriendlyByteBufProxy.class);

    @ConstructorInvoker
    ByteBuf newInstance(ByteBuf parent);
}
