package net.momirealms.craftengine.proxy.minecraft.core;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.core.BlockPos$MutableBlockPos")
public interface MutableBlockPosProxy {
    MutableBlockPosProxy INSTANCE = ASMProxyFactory.create(MutableBlockPosProxy.class);

    @ConstructorInvoker
    Object newInstance();

    @MethodInvoker(name = "setWithOffset")
    Object setWithOffset(Object target, @Type(clazz = Vec3iProxy.class) Object pos, @Type(clazz = DirectionProxy.class) Object direction);

    @MethodInvoker(name = "setWithOffset")
    Object setWithOffset(Object target, @Type(clazz = Vec3iProxy.class) Object pos, int offsetX, int offsetY, int offsetZ);
}
