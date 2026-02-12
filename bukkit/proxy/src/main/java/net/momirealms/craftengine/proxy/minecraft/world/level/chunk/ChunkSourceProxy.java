package net.momirealms.craftengine.proxy.minecraft.world.level.chunk;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.chunk.ChunkSource")
public interface ChunkSourceProxy {
    ChunkSourceProxy INSTANCE = ASMProxyFactory.create(ChunkSourceProxy.class);

    @MethodInvoker(name = "getLightEngine")
    Object getLightEngine(Object target);

    @MethodInvoker(name = "getChunk")
    Object getChunk(Object target, int chunkX, int chunkZ, boolean load);
}
