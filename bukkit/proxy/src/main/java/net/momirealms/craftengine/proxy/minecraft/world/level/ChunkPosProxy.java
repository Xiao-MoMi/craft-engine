package net.momirealms.craftengine.proxy.minecraft.world.level;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.ChunkPos")
public interface ChunkPosProxy {
    ChunkPosProxy INSTANCE = ASMProxyFactory.create(ChunkPosProxy.class);

    @ConstructorInvoker
    Object newInstance(int x, int z);
}
