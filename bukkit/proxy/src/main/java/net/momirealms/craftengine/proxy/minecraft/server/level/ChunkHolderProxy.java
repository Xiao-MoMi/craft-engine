package net.momirealms.craftengine.proxy.minecraft.server.level;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.List;

@ReflectionProxy(name = "net.minecraft.server.level.ChunkHolder")
public interface ChunkHolderProxy {
    ChunkHolderProxy INSTANCE = ASMProxyFactory.create(ChunkHolderProxy.class);

    @MethodInvoker(name = {"moonrise$getPlayers", "getPlayers"})
    List<Object> getPlayers(Object target, boolean onlyOnWatchDistanceEdge);
}
