package net.momirealms.craftengine.bukkit.reflection.minecraft.server.level;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.craftengine.bukkit.reflection.minecraft.world.entity.player.PlayerProxy;
import net.momirealms.craftengine.bukkit.reflection.minecraft.world.inventory.AbstractContainerMenuProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

@ReflectionProxy(name = "net.minecraft.server.level.ServerPlayer")
public interface ServerPlayerProxy extends PlayerProxy {
    ServerPlayerProxy INSTANCE = ReflectionHelper.getProxy(ServerPlayerProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(ServerPlayerProxy.class);

    @FieldGetter(name = "chunkLoader")
    Object chunkLoader(Object instance);

    @FieldSetter(name = "chunkLoader")
    void chunkLoader(Object instance, Object chunkLoader);

    @MethodInvoker(name = "nextContainerCounter")
    int nextContainerCounter(Object instance);

    @MethodInvoker(name = "initMenu")
    void initMenu(Object instance, @Type(clazz = AbstractContainerMenuProxy.class) Object menu);
}
