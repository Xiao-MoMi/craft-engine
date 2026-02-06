package net.momirealms.craftengine.proxy.paper.chunk.system.entity;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "io.papermc.paper.chunk.system.RegionizedPlayerChunkLoader")
public interface RegionizedPlayerChunkLoaderProxy {
    RegionizedPlayerChunkLoaderProxy INSTANCE = ASMProxyFactory.create(RegionizedPlayerChunkLoaderProxy.class);

    @ReflectionProxy(name = "io.papermc.paper.chunk.system.RegionizedPlayerChunkLoader$PlayerChunkLoaderData")
    interface PlayerChunkLoaderDataProxy {
        PlayerChunkLoaderDataProxy INSTANCE = ASMProxyFactory.create(PlayerChunkLoaderDataProxy.class);

        @FieldGetter(name = "sentChunks")
        LongOpenHashSet getSentChunks(Object target);
    }
}
