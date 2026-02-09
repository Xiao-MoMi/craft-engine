package net.momirealms.craftengine.proxy.minecraft.network.chat;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.chat.Component")
public interface ComponentProxy {
    ComponentProxy INSTANCE = ASMProxyFactory.create(ComponentProxy.class);

    @MethodInvoker(name = "empty", isStatic = true)
    Object empty();

    @MethodInvoker(name = "getString")
    String getString(Object target);

    @MethodInvoker(name = "literal", isStatic = true)
    Object literal(String text);
}
