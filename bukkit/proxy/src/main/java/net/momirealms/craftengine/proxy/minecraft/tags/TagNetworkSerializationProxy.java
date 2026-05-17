package net.momirealms.craftengine.proxy.minecraft.tags;

import net.momirealms.craftengine.proxy.minecraft.core.LayeredRegistryAccessProxy;
import net.momirealms.craftengine.proxy.minecraft.network.FriendlyByteBufProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import java.util.Map;

@ReflectionProxy(name = "net.minecraft.tags.TagNetworkSerialization")
public interface TagNetworkSerializationProxy {
    TagNetworkSerializationProxy INSTANCE = ASMProxyFactory.create(TagNetworkSerializationProxy.class);

    @MethodInvoker(name = "serializeTagsToNetwork", isStatic = true)
    Map<Object, Object> serializeTagsToNetwork(@Type(clazz = LayeredRegistryAccessProxy.class) Object registryAccess);

    @ReflectionProxy(name = "net.minecraft.tags.TagNetworkSerialization$NetworkPayload")
    interface NetworkPayloadProxy {
        NetworkPayloadProxy INSTANCE = ASMProxyFactory.create(NetworkPayloadProxy.class);

        @MethodInvoker(name = "write")
        void write(Object target, @Type(clazz = FriendlyByteBufProxy.class) Object buf);

        @MethodInvoker(name = "read", isStatic = true)
        Object read(@Type(clazz = FriendlyByteBufProxy.class) Object buf);
    }
}
