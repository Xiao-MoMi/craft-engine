package net.momirealms.craftengine.proxy.minecraft.world.level.chunk;

import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockStateProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

@ReflectionProxy(name = "net.minecraft.world.level.chunk.LevelChunkSection")
public interface LevelChunkSectionProxy {
    LevelChunkSectionProxy INSTANCE = ASMProxyFactory.create(LevelChunkSectionProxy.class);

    @MethodInvoker(name = "setBlockState")
    Object setBlockState(Object target, int x, int y, int z, @Type(clazz =BlockStateProxy.class) Object blockState);

    @MethodInvoker(name = "setBlockState")
    Object setBlockState(Object target, int x, int y, int z, @Type(clazz = BlockStateProxy.class) Object blockState, boolean useLocks);

    @FieldGetter(name = "states")
    Object getStates(Object target);

    @FieldSetter(name = "states")
    void setStates(Object target, Object states);

    @MethodInvoker(name = "hasOnlyAir")
    boolean hasOnlyAir(Object target);

    @FieldGetter(name = "biomes")
    Object getBiomes(Object target);

    @FieldSetter(name = "biomes")
    void setBiomes(Object target, Object biomes);
}
