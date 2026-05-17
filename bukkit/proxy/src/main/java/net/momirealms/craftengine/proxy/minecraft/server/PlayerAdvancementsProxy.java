package net.momirealms.craftengine.proxy.minecraft.server;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.server.PlayerAdvancements")
public interface PlayerAdvancementsProxy {
    PlayerAdvancementsProxy INSTANCE = ASMProxyFactory.create(PlayerAdvancementsProxy.class);

    @MethodInvoker(name = "save")
    void save(Object target);
}
