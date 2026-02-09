package net.momirealms.craftengine.proxy.minecraft.core;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.core.BlockPos")
public interface BlockPosProxy extends Vec3iProxy {
    BlockPosProxy INSTANCE = ASMProxyFactory.create(BlockPosProxy.class);

    @MethodInvoker(name = "mutable")
    Object mutable(Object target);

    @MethodInvoker(name = "offset")
    Object offset(Object target, int x, int y, int z);
}
