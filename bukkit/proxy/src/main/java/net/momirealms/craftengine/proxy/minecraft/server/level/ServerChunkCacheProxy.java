package net.momirealms.craftengine.proxy.minecraft.server.level;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.server.level.ServerChunkCache")
public interface ServerChunkCacheProxy {
    ServerChunkCacheProxy INSTANCE = ASMProxyFactory.create(ServerChunkCacheProxy.class);

    @MethodInvoker(name = "getGenerator")
    Object getGenerator(Object target);

    @FieldGetter(name = "chunkMap")
    Object getChunkMap(Object target);

    @FieldSetter(name = "chunkMap")
    void getChunkMap(Object target, Object chunkMap);

}
