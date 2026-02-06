package net.momirealms.craftengine.proxy.minecraft.server.players;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.server.players.PlayerList")
public interface PlayerListProxy {
    PlayerListProxy INSTANCE = ASMProxyFactory.create(PlayerListProxy.class);

    @MethodInvoker(name = {"reloadRecipeData", "reloadRecipes"})
    void reloadRecipeData(Object target);
}
