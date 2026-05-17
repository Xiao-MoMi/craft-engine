package net.momirealms.craftengine.proxy.minecraft.network.chat;

import net.momirealms.craftengine.proxy.minecraft.core.RegistryAccessProxy;
import net.momirealms.craftengine.proxy.minecraft.network.FriendlyByteBufProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

import java.util.Optional;

@ReflectionProxy(name = "net.minecraft.network.chat.ChatType")
public interface ChatTypeProxy {
    ChatTypeProxy INSTANCE = ASMProxyFactory.create(ChatTypeProxy.class);

    @ReflectionProxy(name = "net.minecraft.network.chat.ChatType$Bound")
    interface BoundProxy {
        BoundProxy INSTANCE = ASMProxyFactory.create(BoundProxy.class);
        Object STREAM_CODEC = INSTANCE.getStreamCodec();

        @FieldGetter(name = "STREAM_CODEC", isStatic = true, activeIf = "min_version=1.20.5")
        default Object getStreamCodec() {
            return null;
        }

        @MethodInvoker(name = "decorate")
        Object decorate(Object target, @Type(clazz = ComponentProxy.class) Object content);
    }

    @ReflectionProxy(name = "net.minecraft.network.chat.ChatType$BoundNetwork", activeIf = "max_version=1.20.4")
    interface BoundNetworkProxy {
        BoundNetworkProxy INSTANCE = ASMProxyFactory.create(BoundNetworkProxy.class);

        @ConstructorInvoker
        Object newInstance(@Type(clazz = FriendlyByteBufProxy.class) Object buf);

        @MethodInvoker(name = "resolve")
        Optional<Object> resolve(Object target, @Type(clazz = RegistryAccessProxy.class) Object registryManager);
    }
}
