package net.momirealms.craftengine.proxy.minecraft.core;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.core.BlockPos")
public interface BlockPosProxy extends Vec3iProxy {
    BlockPosProxy INSTANCE = ASMProxyFactory.create(BlockPosProxy.class);

    @ConstructorInvoker
    Object newInstance$1(int x, int y, int z); // fixme 无法覆写接口

    @MethodInvoker(name = "mutable")
    Object mutable(Object target);

    @MethodInvoker(name = "offset")
    Object offset(Object target, int x, int y, int z);

    @MethodInvoker(name = "relative")
    Object relative(Object target, @Type(clazz = DirectionProxy.class) Object direction);
}
