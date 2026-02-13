package net.momirealms.craftengine.proxy.minecraft.world.level;

import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.shapes.VoxelShapeProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import java.util.function.Supplier;

@ReflectionProxy(name = "net.minecraft.world.level.BlockGetter")
public interface BlockGetterProxy extends LevelHeightAccessorProxy {
    BlockGetterProxy INSTANCE = ASMProxyFactory.create(BlockGetterProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.BlockGetter");

    @MethodInvoker(name = "getBlockFloorHeight")
    double getBlockFloorHeight(Object target, @Type(clazz = BlockPosProxy.class) Object pos);

    @MethodInvoker(name = "getBlockFloorHeight")
    double getBlockFloorHeight(Object target, @Type(clazz = VoxelShapeProxy.class) Object shape, Supplier<Object> belowShapeSupplier);

    @MethodInvoker(name = "getFluidState")
    Object getFluidState(Object target, @Type(clazz = BlockPosProxy.class) Object blockPos);

    @MethodInvoker(name = "getBlockState")
    Object getBlockState(Object target, @Type(clazz = BlockPosProxy.class) Object blockPos);

    @MethodInvoker(name = "getBlockStateIfLoaded")
    Object getBlockStateIfLoaded(Object target, @Type(clazz = BlockPosProxy.class) Object blockPos);
}
