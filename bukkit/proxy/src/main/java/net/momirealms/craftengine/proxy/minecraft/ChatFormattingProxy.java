package net.momirealms.craftengine.proxy.minecraft;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.ChatFormatting")
public interface ChatFormattingProxy {
    ChatFormattingProxy INSTANCE = ASMProxyFactory.create(ChatFormattingProxy.class);

    @MethodInvoker(name = "valueOf", isStatic = true)
    Object valueOf(String name);
}
