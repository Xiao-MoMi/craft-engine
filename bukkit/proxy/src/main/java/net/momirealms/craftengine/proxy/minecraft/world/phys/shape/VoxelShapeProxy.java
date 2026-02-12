package net.momirealms.craftengine.proxy.minecraft.world.phys.shape;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.phys.shapes.VoxelShape")
public interface VoxelShapeProxy {
    VoxelShapeProxy INSTANCE = ASMProxyFactory.create(VoxelShapeProxy.class);

    @MethodInvoker(name = "bounds")
    Object bounds(Object target);
}
