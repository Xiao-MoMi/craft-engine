package net.momirealms.craftengine.proxy.minecraft.network.chat;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.chat.RemoteChatSession")
public interface RemoteChatSessionProxy {
    RemoteChatSessionProxy INSTANCE = ASMProxyFactory.create(RemoteChatSessionProxy.class);

    interface DataProxy {
        DataProxy INSTANCE = ASMProxyFactory.create(DataProxy.class);

    }
}
