package net.momirealms.craftengine.proxy.minecraft.server.level;

import net.momirealms.craftengine.proxy.minecraft.world.entity.player.PlayerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.inventory.AbstractContainerMenuProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

@ReflectionProxy(name = "net.minecraft.server.level.ServerPlayer")
public interface ServerPlayerProxy extends PlayerProxy {
    ServerPlayerProxy INSTANCE = ASMProxyFactory.create(ServerPlayerProxy.class);

    @FieldGetter(name = "chunkLoader")
    Object getChunkLoader(Object target);

    @FieldGetter(name = "connection")
    Object getConnection(Object target);

    @MethodInvoker(name = "initMenu")
    void initMenu(Object target, @Type(clazz = AbstractContainerMenuProxy.class) Object menu);

    @MethodInvoker(name = "nextContainerCounter")
    int nextContainerCounter(Object target);
}
