package net.momirealms.craftengine.proxy.adventure.text;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net{}kyori{}adventure{}text{}TextComponent", ignoreRelocation = true, activeIf = "has_patch=paper")
public interface TextComponentProxy {
    TextComponentProxy INSTANCE = ASMProxyFactory.create(TextComponentProxy.class);
    Class<?> CLASS = SparrowClass.find("net{}kyori{}adventure{}text{}TextComponent".replace("{}", "."));

    @MethodInvoker(name = "content")
    String content(Object target);
}
