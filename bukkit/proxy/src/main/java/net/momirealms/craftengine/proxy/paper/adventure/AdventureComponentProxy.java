package net.momirealms.craftengine.proxy.paper.adventure;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "io.papermc.paper.adventure.AdventureComponent", activeIf = "has_patch=paper")
public interface AdventureComponentProxy {
    AdventureComponentProxy INSTANCE = ASMProxyFactory.create(AdventureComponentProxy.class);
    Class<?> CLASS = SparrowClass.find("io.papermc.paper.adventure.AdventureComponent");

    @MethodInvoker(name = "adventure$component")
    Object adventureComponent(Object target);
}
