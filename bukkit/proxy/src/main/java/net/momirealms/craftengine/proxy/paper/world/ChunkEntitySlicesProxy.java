package net.momirealms.craftengine.proxy.paper.world;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = {"io.papermc.paper.world.ChunkEntitySlices", "ca.spottedleaf.moonrise.patches.chunk_system.level.entity.ChunkEntitySlices"})
public interface ChunkEntitySlicesProxy {
    ChunkEntitySlicesProxy INSTANCE = ASMProxyFactory.create(ChunkEntitySlicesProxy.class);

    @MethodInvoker(name = "isPreventingStatusUpdates")
    boolean isPreventingStatusUpdates(Object target);
}
