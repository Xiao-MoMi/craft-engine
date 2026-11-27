package net.momirealms.craftengine.proxy.minecraft.server.level;

import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.chunk.ChunkSourceProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

@ReflectionProxy(name = "net.minecraft.server.level.ServerChunkCache")
public interface ServerChunkCacheProxy extends ChunkSourceProxy {
    ServerChunkCacheProxy INSTANCE = ASMProxyFactory.create(ServerChunkCacheProxy.class);

    @MethodInvoker(name = "getGenerator")
    Object getGenerator(Object target);

    @FieldGetter(name = "chunkMap")
    Object getChunkMap(Object target);

    @FieldSetter(name = "chunkMap")
    void getChunkMap(Object target, Object chunkMap);

    @MethodInvoker(name = "blockChanged")
    void blockChanged(Object target, @Type(clazz = BlockPosProxy.class) Object blockPos);

    @MethodInvoker(name = "removeEntity")
    void removeEntity(Object target, @Type(clazz = EntityProxy.class) Object entity);

    @MethodInvoker(name = {"getChunkAtIfLoadedMainThread"}, activeIf = "max_version=1.20.6 && has_patch=paper")
    Object getChunkAtIfLoadedMainThread(Object target, int chunkX, int chunkZ);

    @MethodInvoker(name = {"getChunkAtIfLoadedImmediately"}, activeIf = "has_patch=paper")
    Object getChunkAtIfLoadedImmediately(Object target, int chunkX, int chunkZ);

    @MethodInvoker(name = "getChunkNow")
    Object getChunkNow(Object target, int chunkX, int chunkZ);

    @MethodInvoker(name = "randomState")
    Object randomState(Object target);
}
