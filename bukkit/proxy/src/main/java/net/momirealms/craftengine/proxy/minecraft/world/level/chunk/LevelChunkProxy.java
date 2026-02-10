package net.momirealms.craftengine.proxy.minecraft.world.level.chunk;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.chunk.LevelChunk")
public interface LevelChunkProxy {
    LevelChunkProxy INSTANCE = ASMProxyFactory.create(LevelChunkProxy.class);

    @MethodInvoker(name = "isUnsaved")
    boolean isUnsaved(Object target);

    @MethodInvoker(name = "markUnsaved", activeIf = "min_version=1.21.2")
    void markUnsaved(Object target);

    @MethodInvoker(name = "setUnsaved", activeIf = "max_version=1.21.1")
    void setUnsaved(Object target, boolean needsSaving);
}
