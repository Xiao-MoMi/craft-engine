package net.momirealms.craftengine.proxy.minecraft.world.level.chunk;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.chunk.ChunkAccess")
public interface ChunkAccessProxy {
    ChunkAccessProxy INSTANCE = ASMProxyFactory.create(ChunkAccessProxy.class);

    @MethodInvoker(name = "isUnsaved")
    boolean isUnsaved(Object target);

    @MethodInvoker(name = "getSections")
    Object[] getSections(Object target);

    @MethodInvoker(name = "setUnsaved", activeIf = "max_version=1.21.1")
    void setUnsaved(Object target, boolean needsSaving);
}
