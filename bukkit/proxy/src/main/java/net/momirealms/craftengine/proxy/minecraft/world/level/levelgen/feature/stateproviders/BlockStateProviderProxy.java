package net.momirealms.craftengine.proxy.minecraft.world.level.levelgen.feature.stateproviders;

import com.mojang.serialization.Codec;
import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.util.RandomSourceProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelAccessorProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.WorldGenLevelProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider")
public interface BlockStateProviderProxy {
    BlockStateProviderProxy INSTANCE = ASMProxyFactory.create(BlockStateProviderProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider");

    @FieldGetter(name = "CODEC", isStatic = true)
    Codec<Object> codec();

    @MethodInvoker(name = "getState", activeIf = "max_version=1.21.11")
    Object getState(Object target,
                    @Type(clazz = RandomSourceProxy.class) Object random,
                    @Type(clazz = BlockPosProxy.class) Object pos);

    @MethodInvoker(name = "getState", activeIf = "min_version=26.1 && max_version=26.2")
    Object getStateInWorldGenLevel(Object target,
                                   @Type(clazz = WorldGenLevelProxy.class) Object level,
                                   @Type(clazz = RandomSourceProxy.class) Object random,
                                   @Type(clazz = BlockPosProxy.class) Object pos);

    @MethodInvoker(name = "getState", activeIf = "min_version=26.3")
    Object getStateInLevelAccessor(Object target,
                                   @Type(clazz = LevelAccessorProxy.class) Object level,
                                   @Type(clazz = RandomSourceProxy.class) Object random,
                                   @Type(clazz = BlockPosProxy.class) Object pos);

    @MethodInvoker(name = "getOptionalState", activeIf = "min_version=26.3")
    Object getOptionalState(Object target,
                            @Type(clazz = LevelAccessorProxy.class) Object level,
                            @Type(clazz = RandomSourceProxy.class) Object random,
                            @Type(clazz = BlockPosProxy.class) Object pos);
}
