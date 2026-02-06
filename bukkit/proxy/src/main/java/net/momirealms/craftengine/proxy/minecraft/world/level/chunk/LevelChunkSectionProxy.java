package net.momirealms.craftengine.proxy.minecraft.world.level.chunk;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.chunk.LevelChunkSection")
public interface LevelChunkSectionProxy {
    LevelChunkSectionProxy INSTANCE = ASMProxyFactory.create(LevelChunkSectionProxy.class);

    @MethodInvoker(name = "setBlockState")
    Object setBlockState(Object target, int x, int y, int z, Object blockState);

    @MethodInvoker(name = "setBlockState")
    Object setBlockState(Object target, int x, int y, int z, Object blockState, boolean useLocks);
}
