package net.momirealms.craftengine.proxy.adventure.text;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net{}kyori{}adventure{}text{}Component", ignoreRelocation = true)
public interface ComponentProxy {
    ComponentProxy INSTANCE = ASMProxyFactory.create(ComponentProxy.class);
    Class<?> CLASS = SparrowClass.find("net{}kyori{}adventure{}text{}Component".replace("{}", "."));
}
