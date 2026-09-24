package net.momirealms.craftengine.proxy.spottedleaf.moonrise.patches.chunk_system.scheduling.task;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "ca.spottedleaf.moonrise.patches.chunk_system.scheduling.task.ChunkLoadTask$ReadChunk", activeIf = "min_version=1.21.4 && has_patch=paper")
public interface ReadChunkProxy {
    ReadChunkProxy INSTANCE = ASMProxyFactory.create(ReadChunkProxy.class);

    @MethodInvoker(name = "protoChunk")
    Object protoChunk(Object target);
}
