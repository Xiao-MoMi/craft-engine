package net.momirealms.craftengine.proxy.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.util.RandomSourceProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.WorldGenLevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.chunk.ChunkGeneratorProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.level.levelgen.feature.ConfiguredFeature")
public interface ConfiguredFeatureProxy {
    ConfiguredFeatureProxy INSTANCE = ASMProxyFactory.create(ConfiguredFeatureProxy.class);
    Codec<Object> CODEC = INSTANCE.getCodec();

    @FieldGetter(name = "CODEC", isStatic = true)
    Codec<Object> getCodec();

    @MethodInvoker(name = "place")
    boolean place(Object target,
                  @Type(clazz = WorldGenLevelProxy.class) Object level,
                  @Type(clazz = ChunkGeneratorProxy.class) Object generator,
                  @Type(clazz = RandomSourceProxy.class) Object random,
                  @Type(clazz = BlockPosProxy.class) Object pos);
}
