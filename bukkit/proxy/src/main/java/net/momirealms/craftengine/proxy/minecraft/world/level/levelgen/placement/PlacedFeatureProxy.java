package net.momirealms.craftengine.proxy.minecraft.world.level.levelgen.placement;

import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.core.HolderProxy;
import net.momirealms.craftengine.proxy.minecraft.util.RandomSourceProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.WorldGenLevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.chunk.ChunkGeneratorProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import java.util.List;

@ReflectionProxy(name = "net.minecraft.world.level.levelgen.placement.PlacedFeature")
public interface PlacedFeatureProxy {
    PlacedFeatureProxy INSTANCE = ASMProxyFactory.create(PlacedFeatureProxy.class);

    @ConstructorInvoker
    Object newInstance(@Type(clazz = HolderProxy.class) Object feature, List<Object> placement);

    boolean place(Object target,
                  @Type(clazz = WorldGenLevelProxy.class) Object level,
                  @Type(clazz = ChunkGeneratorProxy.class) Object generator,
                  @Type(clazz = RandomSourceProxy.class) Object random,
                  @Type(clazz = BlockPosProxy.class) Object pos);
}
