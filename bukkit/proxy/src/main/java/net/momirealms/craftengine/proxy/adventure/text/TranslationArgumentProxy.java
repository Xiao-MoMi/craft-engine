package net.momirealms.craftengine.proxy.adventure.text;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net{}kyori{}adventure{}text{}TranslationArgument", ignoreRelocation = true, optional = true, activeIf = "has_patch=paper")
public interface TranslationArgumentProxy {
    TranslationArgumentProxy INSTANCE = ASMProxyFactory.create(TranslationArgumentProxy.class);

    @MethodInvoker(name = "value")
    Object value(Object target);
}
