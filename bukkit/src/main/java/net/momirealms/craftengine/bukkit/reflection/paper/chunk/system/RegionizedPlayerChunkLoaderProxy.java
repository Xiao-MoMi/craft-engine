package net.momirealms.craftengine.bukkit.reflection.paper.chunk.system;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.Strategy;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(names = {
        "ca.spottedleaf.moonrise.patches.chunk_system.player.RegionizedPlayerChunkLoader",
        "io.papermc.paper.chunk.system.RegionizedPlayerChunkLoader"
})
public interface RegionizedPlayerChunkLoaderProxy {
    RegionizedPlayerChunkLoaderProxy INSTANCE = ReflectionHelper.getProxy(RegionizedPlayerChunkLoaderProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(RegionizedPlayerChunkLoaderProxy.class);

    @ReflectionProxy(names = {
            "ca.spottedleaf.moonrise.patches.chunk_system.player.RegionizedPlayerChunkLoader$PlayerChunkLoaderData",
            "io.papermc.paper.chunk.system.RegionizedPlayerChunkLoader$PlayerChunkLoaderData"
    })
    interface PlayerChunkLoaderDataProxy {
        PlayerChunkLoaderDataProxy INSTANCE = ReflectionHelper.getProxy(PlayerChunkLoaderDataProxy.class);
        Class<?> CLAZZ = ReflectionHelper.getClass(PlayerChunkLoaderDataProxy.class);

        @FieldGetter(name = "sentChunks")
        LongOpenHashSet sentChunks(Object instance);

        @FieldSetter(name = "sentChunks", strategy = Strategy.MH)
        void sentChunks(Object instance, LongOpenHashSet sentChunks);
    }
}
