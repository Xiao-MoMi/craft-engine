package net.momirealms.craftengine.proxy.minecraft.server.level;

import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

@ReflectionProxy(name = "net.minecraft.server.level.ServerChunkCache")
public interface ServerChunkCacheProxy {
    ServerChunkCacheProxy INSTANCE = ASMProxyFactory.create(ServerChunkCacheProxy.class);

    @MethodInvoker(name = "getGenerator")
    Object getGenerator(Object target);

    @FieldGetter(name = "chunkMap")
    Object getChunkMap(Object target);

    @FieldSetter(name = "chunkMap")
    void getChunkMap(Object target, Object chunkMap);

    @MethodInvoker(name = "blockChanged")
    void blockChanged(Object target, @Type(clazz = BlockPosProxy.class) Object blockPos);

    @MethodInvoker(name = "getChunk")
    Object getChunk(Object target, int chunkX, int chunkZ, boolean load);

    @MethodInvoker(name = "getChunkAtIfLoadedMainThread", activeIf = "max_version=1.20.6")
    Object getChunkAtIfLoadedMainThread(Object target, int chunkX, int chunkZ);

    @MethodInvoker(name = "getChunkAtIfLoadedImmediately")
    Object getChunkAtIfLoadedImmediately(Object target, int chunkX, int chunkZ);
}
