package net.momirealms.craftengine.proxy.minecraft.network.chat;

import com.google.gson.JsonElement;
import net.momirealms.craftengine.proxy.minecraft.core.HolderLookupProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import javax.annotation.Nullable;

@ReflectionProxy(name = "net.minecraft.network.chat.Component")
public interface ComponentProxy {
    ComponentProxy INSTANCE = ASMProxyFactory.create(ComponentProxy.class);

    @MethodInvoker(name = "empty", isStatic = true)
    Object empty();

    @MethodInvoker(name = "getString")
    String getString(Object target);

    @MethodInvoker(name = "literal", isStatic = true)
    Object literal(String text);

    @ReflectionProxy(name = "net.minecraft.network.chat.Component$Serializer", activeIf = "max_version=1.21.5")
    interface SerializerProxy {
        SerializerProxy INSTANCE = ASMProxyFactory.create(SerializerProxy.class);

        @MethodInvoker(name = "fromJson", isStatic = true, activeIf = "min_version=1.20.5 && max_version=1.21.5")
        Object fromJson(@Nullable JsonElement json, @Type(clazz = HolderLookupProxy.ProviderProxy.class) Object registries);

        @MethodInvoker(name = "fromJson", isStatic = true, activeIf = "max_version=1.20.4")
        Object fromJson(@Nullable JsonElement json);

        @MethodInvoker(name = "fromJson", isStatic = true, activeIf = "min_version=1.20.5 && max_version=1.21.5")
        Object fromJson(String json, @Type(clazz = HolderLookupProxy.ProviderProxy.class) Object registries);

        @MethodInvoker(name = "fromJson", isStatic = true, activeIf = "max_version=1.20.4")
        Object fromJson(String json);

        @MethodInvoker(name = "toJson", isStatic = true, activeIf = "min_version=1.20.5 && max_version=1.21.5")
        String toJson(@Type(clazz = ComponentProxy.class) Object text, @Type(clazz = HolderLookupProxy.ProviderProxy.class) Object registries);

        @MethodInvoker(name = "toJson", isStatic = true, activeIf = "max_version=1.20.4")
        String toJson(@Type(clazz = ComponentProxy.class) Object text);
    }
}
