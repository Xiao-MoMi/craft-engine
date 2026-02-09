package net.momirealms.craftengine.proxy.minecraft.network.chat;

import net.momirealms.craftengine.proxy.minecraft.core.RegistryAccessProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import java.util.Optional;

@ReflectionProxy(name = "net.minecraft.network.chat.ChatType")
public interface ChatTypeProxy {
    ChatTypeProxy INSTANCE = ASMProxyFactory.create(ChatTypeProxy.class);

    @ReflectionProxy(name = "net.minecraft.network.chat.ChatType$Bound")
    interface BoundProxy {
        BoundProxy INSTANCE = ASMProxyFactory.create(BoundProxy.class);

        @MethodInvoker(name = "decorate")
        Object decorate(Object target, @Type(clazz = ComponentProxy.class) Object content);
    }

    @ReflectionProxy(name = "net.minecraft.network.chat.ChatType$BoundNetwork", activeIf = "max_version=1.20.4")
    interface BoundNetworkProxy {
        BoundNetworkProxy INSTANCE = ASMProxyFactory.create(BoundNetworkProxy.class);

        @MethodInvoker(name = "resolve")
        Optional<Object> resolve(Object target, @Type(clazz = RegistryAccessProxy.class) Object registryManager);
    }
}
