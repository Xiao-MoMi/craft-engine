package net.momirealms.craftengine.proxy.minecraft.tags;

import net.momirealms.craftengine.proxy.minecraft.core.LayeredRegistryAccessProxy;
import net.momirealms.craftengine.proxy.minecraft.network.FriendlyByteBufProxy;
import net.momirealms.craftengine.proxy.minecraft.network.codec.StreamCodecProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
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

        @MethodInvoker(name = "write", activeIf = "max_version=26.2")
        default void write(Object target, @Type(clazz = FriendlyByteBufProxy.class) Object buf) {
            StreamCodecProxy.INSTANCE.encode(getStreamCodec(), buf, target);
        }

        @MethodInvoker(name = "read", isStatic = true, activeIf = "max_version=26.2")
        default Object read(@Type(clazz = FriendlyByteBufProxy.class) Object buf) {
            return StreamCodecProxy.INSTANCE.decode(getStreamCodec(), buf);
        }

        @FieldGetter(name = "STREAM_CODEC", isStatic = true, activeIf = "min_version=26.3")
        Object getStreamCodec();
    }
}
