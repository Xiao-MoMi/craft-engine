package net.momirealms.craftengine.proxy.adventure.text;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.List;

@ReflectionProxy(name = "net{}kyori{}adventure{}text{}TranslatableComponent", ignoreRelocation = true, activeIf = "has_patch=paper")
public interface TranslatableComponentProxy {
    TranslatableComponentProxy INSTANCE = ASMProxyFactory.create(TranslatableComponentProxy.class);
    Class<?> CLASS = SparrowClass.find("net{}kyori{}adventure{}text{}TranslatableComponent".replace("{}", "."));

    @MethodInvoker(name = {"arguments", "args"})
    List<Object> arguments(Object target);
}
