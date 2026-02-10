package net.momirealms.craftengine.proxy.minecraft.world.level;

import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.shape.VoxelShapeProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import java.util.function.Supplier;

@ReflectionProxy(name = "net.minecraft.world.level.BlockGetter")
public interface BlockGetterProxy {
    BlockGetterProxy INSTANCE = ASMProxyFactory.create(BlockGetterProxy.class);

    @MethodInvoker(name = "getBlockFloorHeight")
    double getBlockFloorHeight(Object target, @Type(clazz = BlockPosProxy.class) Object pos);

    @MethodInvoker(name = "getBlockFloorHeight")
    double getBlockFloorHeight(Object target, @Type(clazz = VoxelShapeProxy.class) Object shape, Supplier<Object> belowShapeSupplier);

    @MethodInvoker(name = "getFluidState")
    Object getFluidState(Object target, @Type(clazz = BlockPosProxy.class) Object blockPos);
}
